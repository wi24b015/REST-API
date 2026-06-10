# 🔧 Usage Service - Technische Deep Dive

## 📐 Architektur-Übersicht

Das Usage Service ist die **zentrale Verarbeitungskomponente**, die:
1. **Messages aus RabbitMQ** empfängt
2. **Datenbank aktualisiert**
3. **Nachfolgende Services** benachrichtigt

```
┌────────────────────────────────────────────────────────────────────┐
│                    ENERGY PRODUCER & USER                          │
│         (senden PRODUCER/USER messages alle 1-5 sekunden)          │
└─────────────────────────┬──────────────���───────────────────────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │  RabbitMQ Queue       │
              │ energy.messages       │
              │ (persistent=true)     │
              └───────────────────────┘
                          │
                          ▼
        ╔═══════════════════════════════════╗
        ║       USAGE SERVICE (WIR)         ║
        ║  ┌─────────────────────────────┐  ║
        ║  │ MessageListener.java        │  ║ 1. Empfängt Message
        ║  │ @RabbitListener             │  ║ 2. Verarbeitet Logic
        ║  └─────────────────────────────┘  ║ 3. Updated DB
        ║  ┌─────────────────────────────┐  ║ 4. Sendet Update
        ║  │ EnergyUsageRepository.java  │  ║
        ║  │ (JPA/Database)              │  ║
        ║  └─────────────────────────────┘  ║
        ╚═══════════════════════════════════╝
                          │
              ┌───────────┴───────────┐
              ▼                       ▼
        PostgreSQL DB         RabbitMQ Queue
        energy_usage table    energy.updates
                              (UPDATE messages)
                                    │
                                    ▼
                    ┌──────────────────────────┐
                    │ Current Percentage       │
                    │ Service (nächster)       │
                    └──────────────────────────┘
```

---

## 🧩 Komponenten im Detail

### 1. **EnergyUsage.java** - Die Datenbankentität

**Zweck:** Repräsentiert eine Stunde an Energiedaten

```java
@Entity                           // ← JPA Annotation: wird zu DB-Tabelle
@Table(name = "energy_usage")    // ← Tabellenname
public class EnergyUsage {
    
    @Id                          // ← Primärschlüssel
    private String hour;         // ← Format: "2025-01-10T14:00:00"
    
    @Column(name = "community_produced")
    private double communityProduced;  // ← Summe produziert in dieser Stunde
    
    @Column(name = "community_used")
    private double communityUsed;      // ← Summe verbraucht aus Community Pool
    
    @Column(name = "grid_used")
    private double gridUsed;           // ← Summe vom Grid benötigt
}
```

**Datenbank-Mapping:**
```sql
CREATE TABLE energy_usage (
    hour VARCHAR(19) PRIMARY KEY,           -- Primärschlüssel (Hour als String)
    community_produced DOUBLE PRECISION,    -- z.B. 18.05
    community_used DOUBLE PRECISION,        -- z.B. 18.05
    grid_used DOUBLE PRECISION              -- z.B. 1.076
);
```

**Beispiel-Datensätze:**
```
hour                   │ community_produced │ community_used │ grid_used
───────────────────────┼────────────────────┼────────────────┼──────────
2025-01-10T14:00:00    │ 18.05              │ 18.05          │ 1.076
2025-01-10T13:00:00    │ 15.015             │ 14.033         │ 2.049
2025-01-10T12:00:00    │ 25.340             │ 20.125         │ 0.215
```

---

### 2. **RabbitConfig.java** - Messaging-Konfiguration

**Zweck:** Definiert die Verbindung zu RabbitMQ Queues

```java
@Configuration  // ← Spring Configuration Klasse
public class RabbitConfig {
    
    // INPUT QUEUE - von Producer & User
    public static final String ENERGY_MESSAGES_QUEUE = "energy.messages";
    
    // OUTPUT QUEUE - zu nächsten Services
    public static final String ENERGY_UPDATES_QUEUE = "energy.updates";
    
    @Bean  // ← Spring Bean - wird beim Start erstellt
    public Queue energyMessagesQueue() {
        return new Queue(ENERGY_MESSAGES_QUEUE, true);  
        // true = "durable" - Queue bleibt auch bei Restart
    }
    
    @Bean
    public Queue energyUpdatesQueue() {
        return new Queue(ENERGY_UPDATES_QUEUE, true);
    }
}
```

**Queue-Konzept:**
```
┌─────────────────────────────┐
│   energy.messages Queue     │
│   (persistent=true)         │
├────────���────────────────────┤
│                             │
│  [PRODUCER msg] ──┐         │
│  [USER msg]    ──┼→ [Listener wartet hier] ─→ handleEnergyMessage()
│  [PRODUCER msg] ──┘         │
│                             │
│  (Abgelegt von Energy Product./User)  │
└─────────────────────────────┘
```

---

### 3. **MessageListener.java** - Die Kernlogik

**Zweck:** Empfängt Messages und verarbeitet die Energieverteilung

#### Step 1: Message empfangen
```java
@RabbitListener(queues = RabbitConfig.ENERGY_MESSAGES_QUEUE)
public void handleEnergyMessage(EnergyMessage message) {
    // Dieser Code wird AUTOMATISCH aufgerufen,
    // sobald eine Message in der Queue ankommt!
    // RabbitMQ: "Hey, da ist eine neue Message! Jetzt!"
}
```

#### Step 2: Zeitstempel normalisieren
```java
// Eingabe: "2025-01-10T14:33:47"  (von Producer/User)
String hourKey = message.getDatetime().substring(0, 13) + ":00:00";
// Ausgabe: "2025-01-10T14:00:00"  (volle Stunde)

// Grund: Alle Messages einer Stunde werden aggregiert
```

**Beispiel:**
```
14:05:32 → Nachricht ankommt
14:33:47 → Nachricht ankommt
14:59:22 → Nachricht ankommt
           ↓
Alle werden als "2025-01-10T14:00:00" behandelt und aggregiert
```

#### Step 3: Daten aus Datenbank laden oder erstellen
```java
EnergyUsage usage = repository.findById(hourKey)
        .orElseGet(() -> new EnergyUsage(hourKey));
        
// Deutsch:
// - Versuche einen Datensatz mit dieser Stunde zu laden
// - Falls nicht existiert: Erstelle einen neuen (alle Werte = 0.0)
```

**Szenario 1: Erste Message der Stunde**
```
REQUEST: findById("2025-01-10T14:00:00")
DB: Nicht gefunden ❌
ACTION: Erstelle neue EnergyUsage("2025-01-10T14:00:00")
         communityProduced = 0.0
         communityUsed = 0.0
         gridUsed = 0.0
```

**Szenario 2: Weitere Messages der Stunde**
```
REQUEST: findById("2025-01-10T14:00:00")
DB: Gefunden ✅ mit {produced: 5.3, used: 4.2, grid: 0.5}
ACTION: Lade diese Werte
```

#### Step 4: Message-Typ verarbeiten

**Fall A: PRODUCER Message**
```java
if ("PRODUCER".equals(message.getType())) {
    usage.setCommunityProduced(usage.getCommunityProduced() + message.getKwh());
}
```

**Beispiel:**
```
Vor:  communityProduced = 5.3
Msg:  PRODUCER, 0.025 kWh
Nach: communityProduced = 5.3 + 0.025 = 5.325
```

**Fall B: USER Message - DIE KRITISCHE LOGIK**
```java
else if ("USER".equals(message.getType())) {
    // 1. Wieviel Community-Energie ist noch VERFÜGBAR?
    double available = Math.max(0, 
        usage.getCommunityProduced() - usage.getCommunityUsed()
    );
    
    // 2. Ist genug verfügbar?
    if (available >= message.getKwh()) {
        // SZENARIO A: Ja, genug vorhanden → Aus Community Pool
        usage.setCommunityUsed(usage.getCommunityUsed() + message.getKwh());
    } else {
        // SZENARIO B: Nein, nicht genug → Teilweise Community, teilweise Grid
        usage.setCommunityUsed(usage.getCommunityUsed() + available);
        usage.setGridUsed(usage.getGridUsed() + (message.getKwh() - available));
    }
}
```

---

## 📊 Energieverteilungs-Szenarien

### Szenario 1: Genug Community-Energie vorhanden

```
Ausgangslage (vor der USER-Message):
┌─────��────────────────────────────────────┐
│ hour: 2025-01-10T14:00:00                │
│ communityProduced: 18.05 kWh             │
│ communityUsed:     15.00 kWh             │
│ gridUsed:           0.50 kWh             │
└──────────────────────────────────────────┘
                    ↓

Neue User-Message ankommt: 2.50 kWh

Berechnung:
  available = 18.05 - 15.00 = 3.05 kWh
  requested = 2.50 kWh
  
  3.05 >= 2.50? JA! ✅
  
  → communityUsed += 2.50
  → gridUsed BLEIBT gleich
                    ↓

Ergebnis (nach der USER-Message):
┌──────────────────────────────────────────┐
│ hour: 2025-01-10T14:00:00                │
│ communityProduced: 18.05 kWh             │
│ communityUsed:     17.50 kWh  ← +2.50    │
│ gridUsed:           0.50 kWh  ← gleich   │
└──────────────────────────────────────────┘

💡 Interpretation: User brauchte 2.50 kWh, Gemeinschaft produzierte genug, Grid nicht belastet
```

---

### Szenario 2: NICHT genug Community-Energie (KRITISCH)

```
Ausgangslage:
┌──────────────────────────────────────────┐
│ hour: 2025-01-10T14:00:00                │
│ communityProduced: 18.05 kWh             │
│ communityUsed:     17.50 kWh  ← fast voll│
│ gridUsed:           0.50 kWh             │
└──────────────────────────────────────────┘
                    ↓

Neue User-Message ankommt: 2.00 kWh

Berechnung:
  available = 18.05 - 17.50 = 0.55 kWh ← wenig übrig!
  requested = 2.00 kWh
  
  0.55 >= 2.00? NEIN! ❌
  
  → communityUsed += 0.55 (das ist alles was noch verfügbar ist)
  → gridUsed += (2.00 - 0.55) = 1.45 (Rest muss vom Grid kommen)
                    ↓

Ergebnis (nach der USER-Message):
┌──────────────────────────────────────────┐
│ hour: 2025-01-10T14:00:00                │
│ communityProduced: 18.05 kWh             │
│ communityUsed:     18.05 kWh  ← MAXED! ✅
│ gridUsed:           1.95 kWh  ← +1.45    │
└──────────────────────────────────────────┘

💡 Interpretation:
   - Community Pool ist KOMPLETT aufgebraucht (18.05 = 18.05)
   - Community konnte nur 0.55 von der 2.00 kWh anbieten
   - Restliche 1.45 kWh mussten vom Grid kommen (= Kosten!)

   ⚠️  WICHTIG: community_used kann NIEMALS > community_produced sein!
```

---

### Szenario 3: Mehrere Messages in Serie

```
Startzustand (14:00 Uhr):
│ produced = 0.0
│ used = 0.0
│ grid = 0.0

Message 1 um 14:05: PRODUCER +0.03 kWh
│ produced = 0.03 ✅
│ used = 0.0
│ grid = 0.0

Message 2 um 14:06: USER +0.02 kWh
│ available = 0.03 - 0.0 = 0.03 (genug!)
│ produced = 0.03
│ used = 0.02 ✅
│ grid = 0.0 ✅

Message 3 um 14:07: USER +0.04 kWh
│ available = 0.03 - 0.02 = 0.01 (NICHT genug!)
│ used = 0.02 + 0.01 = 0.03 ✅
│ grid = 0.0 + (0.04-0.01) = 0.03 ⚠️

Message 4 um 14:08: PRODUCER +0.05 kWh
│ produced = 0.03 + 0.05 = 0.08 ✅ (MEHR verfügbar jetzt!)
│ used = 0.03 (bleibt gleich)
│ grid = 0.03 (bleibt gleich)

Final (14:00 Stunde):
│ produced = 0.08 kWh
│ used = 0.03 kWh
│ grid = 0.03 kWh ← Notwendig wegen Spitzenlast
```

---

## 💾 Step 5: Datenbank speichern

```java
EnergyUsage saved = repository.save(usage);
```

**Was passiert:**
```
Nach allen Änderungen:
usage = {
    hour: "2025-01-10T14:00:00",
    communityProduced: 18.05,
    communityUsed: 17.50,
    gridUsed: 1.95
}

↓ (SQL INSERT oder UPDATE)

UPDATE energy_usage 
SET community_produced = 18.05,
    community_used = 17.50,
    grid_used = 1.95
WHERE hour = '2025-01-10T14:00:00';

↓

DB speichert persistent
👍 Data bleibt auch nach Crash erhalten
```

---

## 📤 Step 6: Update-Nachricht senden

```java
private void sendUpdateMessage(EnergyUsage usage) {
    EnergyMessage updateMessage = new EnergyMessage(
            "UPDATE",              // ← Type: UPDATE (nicht PRODUCER/USER)
            "COMMUNITY",           // ← Association
            0.0,                   // ← kwh: nicht relevant für UPDATE
            usage.getHour()        // ← Zeitstempel der Stunde
    );
    
    rabbitTemplate.convertAndSend(
            RabbitConfig.ENERGY_UPDATES_QUEUE,  // ← energy.updates
            updateMessage
    );
}
```

**Nachricht in der Queue:**
```json
{
    "type": "UPDATE",
    "association": "COMMUNITY",
    "kwh": 0.0,
    "datetime": "2025-01-10T14:00:00"
}
```

**Warum?**
```
Usage Service hat fertig:
  ✅ Message verarbeitet
  ✅ DB aktualisiert
  ✅ Energieverteilung berechnet

Nächste Frage: "Wieviel % ist die Community jetzt aufgebraucht?"
  
UPDATE Message sagt: "Hey Current Percentage Service!
                       Die Daten für 14:00 Uhr haben sich geändert!
                       Berechne bitte die neuen Prozentangaben!"

Calculation in Current Percentage Service:
  community_depleted = (17.50 / 18.05) * 100 = 97.0%
  total = 17.50 + 1.95 = 19.45 kWh
  grid_portion = (1.95 / 19.45) * 100 = 10.0%
```

---

## 🔌 Konfiguration in application.properties

```properties
# NAME DES SERVICES
spring.application.name=usage-service

# ────────── RABBITMQ VERBINDUNG ──────────
spring.rabbitmq.host=localhost        # Wo läuft RabbitMQ?
spring.rabbitmq.port=5672             # Port für AMQP Protocol
spring.rabbitmq.username=guest         # Default RabbitMQ User
spring.rabbitmq.password=guest         # Default RabbitMQ Password

# ────────── POSTGRESQL VERBINDUNG ──────────
spring.datasource.url=jdbc:postgresql://localhost:5432/energydb
  # jdbc:postgresql://HOST:PORT/DATENBANKNAME
  
spring.datasource.username=disysuser   # DB User
spring.datasource.password=disyspw     # DB Password
spring.datasource.driver-class-name=org.postgresql.Driver

# ────────── HIBERNATE (ORM) EINSTELLUNGEN ──────────
spring.jpa.hibernate.ddl-auto=update
  # update = Erstelle Tabellen automatisch wenn nicht vorhanden
  # update = Aktualisiere beim Start
  
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
  # Sagt Hibernate: "Wir sind auf PostgreSQL, verwende die Dialekt-Regeln"
```

---

## 🌊 Gesamter Datenfluss - Visual

```
ZEIT 14:35:12 - Producer sendet Message:
┌─────────────────────────────────────────────────────────────┐
│ {                                                           │
│   "type": "PRODUCER",                                       │
│   "association": "COMMUNITY",                               │
│   "kwh": 0.0275,                                            │
│   "datetime": "2025-01-10T14:35:12"                         │
│ }                                                           │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼ (RabbitMQ Network)
              ┌────────────────────┐
              │ energy.messages    │
              │ Queue - RabbitMQ   │
              └────────┬───────────┘
                       │
                       ▼ (Spring Auto-Detects)
            ┌──────────────────────────┐
            │ @RabbitListener Method   │
            │ handleEnergyMessage()    │
            └──────────┬───────────────┘
                       │
         ┌─────────────┴──────────────┐
         ▼                            ▼
    ┌─────────────────────┐   ┌──────────────────┐
    │ Extract Hour        │   │ Load/Create      │
    │ 14:35:12 → 14:00:00│   │ EnergyUsage obj  │
    └──────────┬──────────┘   └────────┬─────────┘
               │                       │
               └───────────┬───────────┘
                           ▼
                   ┌─────────────────────┐
                   │ IF PRODUCER MESSAGE │
                   │ communityProduced  │
                   │   += 0.0275 kWh    │
                   └──────────┬──────────┘
                               │
                   ┌───────────▼──────────┐
                   │ repository.save()    │
                   │ UPDATE zu PostgreSQL │
                   └──────────┬───────────┘
                               │
              ┌────────────────┴──────────────┐
              ▼                               ▼
         ┌──────────────────┐        ┌────────────────────┐
         │ energy_usage     │        │ RabbitTemplate     │
         │ Tabelle UPDATE   │        │ Sende UPDATE Msg   │
         └──────────────────┘        └────────┬───────────┘
                                              │
                                 ┌────────────▼──────────┐
                                 │ energy.updates Queue  │
                                 │ [UPDATE Message]      │
                                 └───────────┬───────────┘
                                             │
                           ┌─────────────────▼──────────┐
                           │ Current Percentage Service │
                           │ (nächster Schritt)         │
                           └────────────────────────────┘
```

---

## 🧪 Fehlerbehandlung

```java
try {
    // Verarbeite Message
    // ...
} catch (Exception e) {
    System.err.println("Error processing message: " + e.getMessage());
    e.printStackTrace();
}
```

**What könnte schiefgehen?**

| Problem | Fehler | Resultat |
|---------|---------|---------| 
| RabbitMQ nicht erreichbar | `AMQPException` | Service startet nicht |
| PostgreSQL nicht verbunden | `SQLException` | `save()` schlägt fehl |
| Ungültiges DateTime-Format | `StringIndexOutOfBoundsException` | Message wird geloggt, weggeworfen |
| Repository.save() Fehler | `JpaSystemException` | Exception geloggt, Message verloren |

**Verbesserungsmöglichkeiten für Production:**
```java
// Dead Letter Queue
// Retry Mechanismus
// Monitoring/Alerting
// Transaktionen mit Rollback
```

---

## 🎯 Zusammenfassung

**Usage Service = Der Prozessor der Energiegemeinschaft:**

| Phase | Was | Wo |
|-------|-----|-----|
| **Input** | PRODUCER/USER Messages | energy.messages Queue |
| **Verarbeitung** | Energy Distribution Logic | MessageListener.java |
| **Speicherung** | Aggregiert nach Stunde | PostgreSQL energy_usage Tabelle |
| **Output** | UPDATE Benachrichtigungen | energy.updates Queue |
| **Konsequenz** | Nächster Service wird benachrichtigt | Current Percentage Service |

**Die kritische Formel:**
```
community_available = community_produced - community_used

if (community_available >= user_request)
    → user_request vollständig aus Community
else
    → (community_available) aus Community
    → (user_request - community_available) vom Grid
```

**Invarianten (Regeln die immer gelten):**
```
✅ community_used ≤ community_produced (unmöglich mehr zu verbrauchen als produziert)
✅ grid_used ≥ 0 (Negativ macht keinen Sinn)
✅ community_produced ≥ 0 (Energiedaten sind positiv)
```

---

*Diese technische Dokumentation erklärt das vollständige Design und die Implementierung des Usage Service für die Energy Community.*

