# 📚 Usage Service - Dokumentations-Index

Ich habe **4 detaillierte technische Dokumentationen** über das Usage Service erstellt:

## 📄 Dokumentations-Übersicht

### 1. **USAGE_SERVICE_DEEP_DIVE.md** ⭐⭐⭐
**Für: Vollständiges technisches Verständnis**

- **Architektur-Übersicht** mit System-Diagramm
- **EnergyUsage.java** - JPA Entity & Datenbankschema
- **RabbitConfig.java** - Queue-Konfiguration
- **MessageListener.java** - Komponenten-Details
- **Application.properties** - Konfigurationsoptionen
- **3 realistische Szenarien:**
  - Szenario 1: Genug Community-Energie
  - Szenario 2: Nicht genug (Grid-Nutzung)
  - Szenario 3: Mehrere Messages in Serie
- **Fehlerbehandlung & Debugging**
- **Invarianten** (Regeln die immer gelten)

✅ **Mit vielen Diagrammen und Beispiel-Datenbankeinträgen**

---

### 2. **USAGE_SERVICE_EXECUTION_FLOW.md** 🎬
**Für: Step-by-Step Code-Ausführung verstehen**

- **Realistes Szenario: Power Peak um 18:00 Uhr**
- **4 Phasen mit exaktem Code-Flow:**
  - 18:00:05 - Erste Producer-Message
  - 18:00:15 - Erste User-Message
  - 18:00:25 - Zweite User-Message (Peak!)
  - 18:00:35 - Producer rettet den Tag!
  
- **Für jede Phase:**
  - Eingabe (Message Content)
  - Code-Ausführung (Zeile für Zeile)
  - SQL-Statements
  - DB-Zustand VOR und NACH
  
- **Metriken-Berechnung:**
  - Wie From rohen Daten zu Business-Metriken
  
- **Daten-Integration:**
  - Wie folgende Services die Daten nutzen

✅ **Mit SQL-Code, Variablen-Werten, DB-Snapshots**

---

### 3. **USAGE_SERVICE_VISUAL_GUIDE.md** 🎨
**Für: Visual Learners & Deployment**

- **System-Komponenten im Überblick**
  - ASCII-Art Diagramme
  - Datenfluss-Pfeile
  
- **Kritische Business Logic (Energieverteilung)**
  - Visuelle Darstellung: Community Production Bucket
  - CASE 1: Small Load (alles aus Community)
  - CASE 2: Large Load (Spaltung Community/Grid)
  - CASE 3: High Demand (Community maxed out)
  
- **Data Flow Timeline-View**
  - Sekunde für Sekunde Darstellung
  - Welche DB-Updates wann
  
- **Invarianten (Constraints)**
  - 4 mathematische Regeln
  - Warum richtig Eingabe = validierte Output
  
- **Deployment Checklist**
  - Was muss laufen (PostgreSQL, RabbitMQ)
  - Wie Usage Service starten
  - Verifikation
  
- **Error Scenarios & Debugging**
  - 6 häufige Fehler
  - Wie zu debuggen

✅ **Ideal für Präsentation & Troubleshooting**

---

### 4. **SITZUNGSPROTOKOLL.md** 📋
**Für: Historisches Record der Entwicklung**

- Was wurde heute alles gemacht
- Phase 1: Projektanalyse
- Phase 2: Implementation Details
- Phase 3: Tests & Validation
- Komponenten-Status
- Nächste Aufgaben
- Fortschrittsmetriken

✅ **Offizielles Entwicklungs-Protokoll**

---

## 🎯 Für verschiedene Zielgruppen

| Zielgruppe | Lies zuerst | Dann | Dann |
|-----------|-------------|------|------|
| **Programmierer** | Deep Dive | Execution Flow | Visual Guide |
| **Projektmanager** | Sitzungsprotokoll | Visual Guide | Deep Dive |
| **Studenten** | Sitzungsprotokoll | Visual Guide | Execution Flow |
| **Reviewer/Betreuer** | Deep Dive + Execution | Visual Guide | Sitzungsprotokoll |
| **DevOps Engineer** | Visual Guide (Deployment) | Deep Dive | Execution Flow |

---

## 📖 Reading Sequence

### 🤔 "Ich verstehe nicht, wie das funktioniert"
1. USAGE_SERVICE_VISUAL_GUIDE.md - System-Komponenten & Bucket visualization
2. USAGE_SERVICE_EXECUTION_FLOW.md - Step-by-step example
3. USAGE_SERVICE_DEEP_DIVE.md - Alle technischen Details

### 🏗️ "Ich muss es deployen"
1. USAGE_SERVICE_VISUAL_GUIDE.md - Deployment Checklist
2. USAGE_SERVICE_DEEP_DIVE.md - application.properties erklärt
3. SITZUNGSPROTOKOLL.md - Status & nächste Schritte

### 🎓 "Ich muss es im Team erklären"
1. USAGE_SERVICE_VISUAL_GUIDE.md - Zeige Component Diagram
2. USAGE_SERVICE_VISUAL_GUIDE.md - Zeige Energy Bucket Scenarios
3. USAGE_SERVICE_EXECUTION_FLOW.md - Live-Demo mit Beispiel durchgehen

### 🐛 "Etwas funktioniert nicht"
1. USAGE_SERVICE_VISUAL_GUIDE.md - Error Scenarios Section
2. USAGE_SERVICE_DEEP_DIVE.md - Fehlerbehandlung
3. USAGE_SERVICE_EXECUTION_FLOW.md - Debug mit ähnlichem Szenario

---

## 🔍 Schnelle Referenzen

### Kernklassen in der Reihenfolge der Ausführung:

```
1. RabbitConfig.java
   ↓ (Definiert Queues)
   
2. MessageListener.java
   ↓ (Empfängt Messages)
   ├─ EnergyMessage.java (Deserialisiert)
   │  ↓
   ├─ EnergyUsage.java (Lädt/erstellt)
   │  ↓ (über Repository)
   │
   └─ EnergyUsageRepository.java
      ↓ (speichert in DB)
      └─ PostgreSQL (energy_usage Tabelle)
```

### Die 6 kritischen Code Lines:

| Zeile | Was | Warum wichtig |
|-------|-----|--------------|
| 21 | @RabbitListener | Definiert Auto-Consumer |
| 25 | `substring(0,13) + ":00:00"` | Normalisiert ZEITemporalität |
| 28-29 | `orElseGet(() -> new ...)` | Erstellt bei erste mal |
| 35 | `Math.max(0, ...)` | Verhindert negative Werte |
| 37-43 | if/else Logic | **KRITISCH: Energieverteilung** |
| 48 | `repository.save()` | Speichert in Datenbank |
| 69-72 | `rabbitTemplate.send()` | Benachrichtigt nächste Service |

---

## ✅ Zusammenfassung

**Sie haben jetzt Dokumentationen für:**

✅ Wie das System aktuell funktioniert  
✅ Warum es so designed ist  
✅ Wie es step-by-step ausführt  
✅ Was möglich Break könnte  
✅ Wie zu deployen & maintainen  
✅ Wie zu erklären & zu lehren  

**Diese Dokumente sind im Projektverzeichnis verfügbar:**
```
C:\Users\atia_\Desktop\semesterprojekt\
├── SITZUNGSPROTOKOLL.md
├── USAGE_SERVICE_DEEP_DIVE.md
├── USAGE_SERVICE_EXECUTION_FLOW.md
└── USAGE_SERVICE_VISUAL_GUIDE.md
```

---

**Nächste Stufe: Current Percentage Service implementieren** 🚀

*Diese Dokumentation wurde am 10. Juni 2026 erstellt und ist Teil der Final Submission.*

