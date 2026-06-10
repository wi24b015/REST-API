# 🎬 Usage Service - Code Execution Flow mit Beispielen

## Realistisches Szenario: Power Peak um 18:00 Uhr

Folgendes passiert in einer Minute zwischen 18:00-18:01 Uhr:

### ⏰ 18:00:05 - Erste Producer-Message

```
MESSAGE ANKOMMT IN QUEUE:
─────────────────────────────────────────────────────────
{
  "type": "PRODUCER",
  "association": "COMMUNITY", 
  "kwh": 0.02,
  "datetime": "2025-01-10T18:00:05"
}

EXECUTION IN USAGE SERVICE:
────────────────────────────────────────────────────────

MessageListener.handleEnergyMessage() wird aufgerufen:

┌─ Line 22-25 ──────────────────────────────────────────┐
│ // Extract hour                                        │
│ String hourKey = "2025-01-10T18:00:05"               │
│   .substring(0, 13) + ":00:00"                         │
│ // hourKey = "2025-01-10T18:00:00"                     │
└────────────────────────────────────────────────────────┘

┌─ Line 28-29 ──────────────────────────────────────────┐
│ EnergyUsage usage = repository.findById("...")         │
│   .orElseGet(() -> new EnergyUsage("..."))             │
│                                                        │
│ DB-Query: SELECT * FROM energy_usage                   │
│           WHERE hour = '2025-01-10T18:00:00'           │
│                                                        │
│ Resultat: NICHT GEFUNDEN (erste Message der Stunde)   │
│                                                        │
│ Action: Erstelle neue instanz:                         │
│   new EnergyUsage("2025-01-10T18:00:00") {             │
│     hour = "2025-01-10T18:00:00"                       │
│     communityProduced = 0.0                            │
│     communityUsed = 0.0                                │
│     gridUsed = 0.0                                     │
│   }                                                    │
└────────────────────────────────────────────────────────┘

┌─ Line 32-33 ──────────────────────────────────────────┐
│ // Message ist PRODUCER                                │
│ if ("PRODUCER".equals("PRODUCER")) {                   │
│   usage.setCommunityProduced(0.0 + 0.02)               │
│   // communityProduced = 0.02 kWh                      │
│ }                                                      │
└────────────────────────────────────────────────────────┘

┌─ Line 48 ─────────────────────────────────────────────┐
│ EnergyUsage saved = repository.save(usage)             │
│                                                        │
│ SQL: INSERT INTO energy_usage                          │
│      (hour, community_produced, community_used, grid_used)
│      VALUES ('2025-01-10T18:00:00', 0.02, 0.0, 0.0)   │
│                                                        │
│ DB-Status NACH: ✅ Row eingefügt                       │
└────────────────────────────────────────────────────────┘

┌─ Line 51-52 ──────────────────────────────────────────┐
│ sendUpdateMessage(saved) wird aufgerufen               │
│                                                        │
│ EnergyMessage updateMsg = new EnergyMessage(           │
│   "UPDATE",                                            │
│   "COMMUNITY",                                         │
│   0.0,                                                 │
│   "2025-01-10T18:00:00"                                │
│ )                                                      │
│                                                        │
│ rabbitTemplate.convertAndSend(                         │
│   "energy.updates",         // ← ANDERE QUEUE!         │
│   updateMsg                 // ← JSON serialisiert     │
│ )                                                      │
│                                                        │
│ Resultat: Message in energy.updates Queue              │
└────────────────────────────────────────────────────────┘

┌─ Line 53 ─────────────────────────────────────────────┐
│ System.out.println(                                    │
│   "Processed message: PRODUCER - 0.02 kWh at ..."      │
│ )                                                      │
│ // STDOUT: Processed message: PRODUCER - 0.02 kWh ... │
└────────────────────────────────────────────────────────┘

DB NACH MESSAGE 1:
─────────────────────────────────────────────────────────
energy_usage table:
  hour=2025-01-10T18:00:00 │ produced=0.02 │ used=0.0 │ grid=0.0
```

---

### ⏰ 18:00:15 - Erste User-Message

```
MESSAGE ANKOMMT IN QUEUE:
─────────────────────────────────────────────────────────
{
  "type": "USER",
  "association": "COMMUNITY", 
  "kwh": 0.015,               ← Verbraucht 0.015 kWh
  "datetime": "2025-01-10T18:00:15"
}

EXECUTION:
───────��────────────────────────────────────────────────

┌─ Line 25 ──────────────���──────────────────────────────┐
│ hourKey = "2025-01-10T18:00:00"                        │
└────────────────────────────────────────────────────────┘

┌─ Line 28-29 ──────────────────────────────────────────┐
│ EnergyUsage usage = repository.findById(...)           │
│   .orElseGet(...)                                      │
│                                                        │
│ DB-Query: SELECT * FROM energy_usage                   │
│           WHERE hour = '2025-01-10T18:00:00'           │
│                                                        │
│ Resultat: GEFUNDEN! (von Message 1)                    │
│ {                                                      │
│   hour = "2025-01-10T18:00:00"                         │
│   communityProduced = 0.02 ← VON VOR 10 SEKUNDEN       │
│   communityUsed = 0.0                                  │
│   gridUsed = 0.0                                       │
│ }                                                      │
└────────────────────────────────────────────────────────┘

┌─ Line 34-44 ──────────────────────────────────────────┐
│ // Message ist USER (nicht PRODUCER)                   │
│ else if ("USER".equals("USER")) {                      │
│   // KRITISCHE BERECHNUNG:                             │
│   double available = Math.max(0,                       │
│     0.02 - 0.0  // communityProduced - communityUsed  │
│   )                                                    │
│   // available = 0.02                                  │
│                                                        │
│   if (0.02 >= 0.015) {  // ← JA, GENUG!                │
│     usage.setCommunityUsed(0.0 + 0.015)                │
│     // communityUsed = 0.015                           │
│     // gridUsed BLEIBT 0.0 ← WICHTIG!                  │
│   } else { ... }                                       │
│ }                                                      │
└────────────────────────────────────────────────────────┘

NACH VERARBEITUNG:
    usage {
      hour = "2025-01-10T18:00:00"
      communityProduced = 0.02
      communityUsed = 0.015    ← +0.015
      gridUsed = 0.0           ← unverändert
    }

┌─ Line 48 ─────────────────────────────────────────────┐
│ repository.save(usage):                                │
│                                                        │
│ SQL: UPDATE energy_usage                               │
│      SET community_used = 0.015,                       │
│          grid_used = 0.0                               │
│      WHERE hour = '2025-01-10T18:00:00'                │
│                                                        │
│ DB-Status NACH: ✅ Row aktualisiert                    │
└────────────────────────────────────────────────────────┘

┌─ Line 51 ─────────────────────────────────────────────┐
│ sendUpdateMessage() → energy.updates Queue             │
│ [2. UPDATE Message]                                    │
└────────────────────────────────────────────────────────┘

DB NACH MESSAGE 2:
─────────────────────────────────────────────────────────
energy_usage table:
  hour=2025-01-10T18:00:00 │ produced=0.02 │ used=0.015 │ grid=0.0
```

---

### ⏰ 18:00:25 - Zweite User-Message (Peak!)

```
MESSAGE ANKOMMT IN QUEUE:
─────────────────────────────────────────────────────────
{
  "type": "USER",
  "association": "COMMUNITY", 
  "kwh": 0.025,               ← Größere Last!
  "datetime": "2025-01-10T18:00:25"
}

EXECUTION:
────────────────────────────────────────────────────────

┌─ Line 28-29 ────────────���─────────────────────────────┐
│ EnergyUsage usage = repository.findById(...)           │
│                                                        │
│ DB-Status VOR dieser Message:                          │
│ {                                                      │
│   hour = "2025-01-10T18:00:00"                         │
│   communityProduced = 0.02                             │
│   communityUsed = 0.015                                │
│   gridUsed = 0.0                                       │
│ }                                                      │
└────────────────────────────────────────────────────────┘

┌─ Line 34-44 "USER MESSAGE PROCESSING" ────────────────┐
│                                                        │
│ double available = Math.max(0,                         │
│   0.02 - 0.015  // Nur noch 0.005 kWh übrig!          │
│ )                                                      │
│ // available = 0.005 kWh ← SEHR WENIG!                 │
│                                                        │
│ if (0.005 >= 0.025) {  // ← NEIN! Nicht genug!         │
│   // ELSE BRANCH:                                      │
│   usage.setCommunityUsed(0.015 + 0.005)                │
│   // communityUsed = 0.020 (Pool komplett leer!)       │
│                                                        │
│   usage.setGridUsed(0.0 + (0.025 - 0.005))             │
│   // gridUsed = 0.0 + 0.020 = 0.020 ← MUSS VOM GRID!   │
│ }                                                      │
│                                                        │
│ 💡 INTERPRETATION:                                     │
│    Community konnte nur 0.005 von 0.025 anbieten       │
│    Restliche 0.020 kWh mussten vom Grid kommen!        │
│    Das kostet Geld und ist ineffizient!                │
└────────────────────────────────────────────────────────┘

NACH VERARBEITUNG:
    usage {
      hour = "2025-01-10T18:00:00"
      communityProduced = 0.02          ← unverändert
      communityUsed = 0.020             ← +0.005 (NUR verfügbar)
      gridUsed = 0.020                  ← +0.020 (VOM GRID!)
    }

PER RULE: communityUsed ≤ communityProduced
          0.020 ≤ 0.02  ✅ VALID (an der Grenze)

┌─ Line 48 ─────────────────────────────────────────────┐
│ repository.save(usage):                                │
│                                                        │
│ SQL: UPDATE energy_usage                               │
│      SET community_used = 0.020,                       │
│          grid_used = 0.020                             │
│      WHERE hour = '2025-01-10T18:00:00'                │
└────────────────────────────────────────────────────────┘

DB NACH MESSAGE 3:
─────────────────────────────────────────────────────────
energy_usage table:
  hour=2025-01-10T18:00:00 │ produced=0.02 │ used=0.020 │ grid=0.020

⚠️  BEACHTE: grid_used ist jetzt NICHT NULL!
             Energiegemeinschaft konnte nicht die komplette Last decken
```

---

### ⏰ 18:00:35 - Producer rettet den Tag!

```
MESSAGE ANKOMMT IN QUEUE:
─────────────────────────────────────────────────────────
{
  "type": "PRODUCER",
  "association": "COMMUNITY", 
  "kwh": 0.035,               ← Sonne scheint intensiver!
  "datetime": "2025-01-10T18:00:35"
}

VORHER im DB:
  produced = 0.02 (zu wenig)
  used = 0.020
  grid = 0.020 (muss vom Netz)

EXECUTION:
────────────────────────────────────────────────────────

┌─ Line 32 ─────────────────────────────────────────────┐
│ if ("PRODUCER".equals("PRODUCER")) {                   │
│   usage.setCommunityProduced(0.02 + 0.035)             │
│   // communityProduced = 0.055 kWh ← VIEL MEHR!        │
│ }                                                      │
└────────────────────────────────────────────────────────┘

NACH VERARBEITUNG:
    usage {
      hour = "2025-01-10T18:00:00"
      communityProduced = 0.055         ← +0.035 (Große Produktion!)
      communityUsed = 0.020             ← unverändert
      gridUsed = 0.020                  ← unverändert (zu spät...)
    }

📊 NEUE SITUATION:
   Jetzt würde die Community die ganze Last decken können!
   Aber gridUsed bleibt auf 0.020, weil das bereits
   verbraucht/beglichen wurde
   
   Nächste User-Message könnte wieder gratis aus Community kommen!

DB NACH MESSAGE 4:
─────────────────────────────────────────────────────────
energy_usage table:
  hour=2025-01-10T18:00:00 │ produced=0.055 │ used=0.020 │ grid=0.020

ANALYSE:
  - Verfügbar: 0.055 - 0.020 = 0.035 kWh (40% der neuen Produktion)
  - Grid-Anteil: 0.020 / (0.020 + 0.020) = 50% der Gesamtlast kam vom Grid
```

---

## 📊 Von Rohdaten zu Geschäftsmetriken

```
Raw DB Data nach dieser Minute:
┌─────────────────────────────────────────┐
│ 2025-01-10T18:00:00                     │
│ ────────────────────────────────────────│
│ communityProduced   = 0.055 kWh         │
│ communityUsed       = 0.020 kWh         │
│ gridUsed            = 0.020 kWh         │
└─────────────────────────────────────────┘
                    ↓
    Current Percentage Service (nächster Schritt)
    wartet auf UPDATE Messages
                    ↓
Berechnet Metriken:
┌─────────────────────────────────────────┐
│ 2025-01-10T18:00:00 - Percentages       │
│ ────────────────────────────────────────│
│ total_energy = 0.020 + 0.020 = 0.040    │
│                                         │
│ community_depleted = 0.020 / 0.055      │
│                   = 36.36%  ← Anteil    │
│                     der Produktion      │
│                                         │
│ grid_portion = 0.020 / 0.040            │
│             = 50.0%   ← Anteil vom Grid │
│               der Gesamtlast             │
└─────────────────────────────────────────┘
                    ↓
            GUI zeigt an:
    "Community: 36% | Grid: 50%"
```

---

## 🔄 Was passiert mit diesen Daten?

```
USAGE SERVICE (wir!)
  ↓
  └─→ UPDATE Message: "18:00 Daten geändert!"
      ↓
      energy.updates Queue
      ↓
      CURRENT PERCENTAGE SERVICE (nächster)
      ↓
      Speichert Percentages in DB
      ↓
      Table: current_percentage
      {
        hour: "2025-01-10T18:00:00"
        community_depleted: 36.36
        grid_portion: 50.0
      }
      ↓
      REST API kann jetzt Daten abrufen
      ↓
      GET /energy/current
      ↓
      GUI zeigt Echtzeit-Daten an:
      
      ┌─────────────────────┐
      │ Community Pool: 36%  │
      │ Grid Portion: 50%    │
      │                      │
      │ [REFRESH]           │
      └─────────────────────┘
```

---

## 🎯 Zusammenfassung dieser 35 Sekunden

| Zeit | Event | Result | Community | Grid |
|------|-------|--------|-----------|------|
| 18:00:05 | Producer +0.02 | Create Row | 0.02 (0%) | 0.0 |
| 18:00:15 | User -0.015 | ✅ From Community | 0.015 | 0.0 |
| 18:00:25 | User -0.025 | ⚠️ Need Grid! | 0.020 | 0.020 |
| 18:00:35 | Producer +0.035 | Plenty now | 0.055 | 0.020 |
| **FINAL** | **Hour Stats** | **Stabile Lage** | **0.020/0.055 (36%)** | **50%** |

---

*Diese detaillierte Nachverfolgung zeigt, wie jede Sekunde des Prozesses funktioniert und welche Business-Entscheidungen das System trifft.*

