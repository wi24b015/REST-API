# 🎨 Usage Service - Visual Architecture Guide

## 1️⃣ Systemkomponenten im Überblick

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         ENERGIEGEMEINSCHAFT                             │
│                                                                         │
│  ┌──────────────┐                            ┌──────────────┐          │
│  │   PRODUCER   │  Every 1-5 sec              │    USER      │          │
│  │              │  + 0.01-0.05 kWh            │              │          │
│  │  [☀️ Solar]   │                            │  [💡 Devices]│          │
│  └──────┬───────┘                            └──────┬───────┘          │
│         │                                           │                   │
│         │ {type: PRODUCER,                          │ {type: USER,      │
│         │  kwh: 0.025,                              │  kwh: 0.03,       │
│         │  datetime: 2025-01-10T14:35}              │  datetime: ...}   │
│         │                                           │                   │
│         └──────────────┬──────────────────────────┬─┘                   │
│                        │                          │                     │
│                        ▼                          ▼                     │
│            ╔═══════════════════════════════════════════╗                │
│            ║   RabbitMQ Message Queue                 ║                │
│            ║   📨 energy.messages (persistent)        ║                │
│            ║                                         ║                │
│            ║  [ PRODUCER,0.025 ]                     ║                │
│            ║  [ USER,0.03 ]                          ║                │
│            ║  [ PRODUCER,0.02 ]     ← RabbitMQ holds║ │                │
│            ║  [ USER,0.025 ]        ← & distributes │ │                │
│            ║                                         ║                │
│            ╚═══════════════════════════════════════════╝                │
│                                                                         │
│                            USAGE SERVICE                                │
│                    ╔════════════════════════╗                           │
│                    ║ @RabbitListener        ║                           │
│                    ║ [Auto-consumes msgs]   ║                           │
│                    ╚════════════════════════╝                           │
│                            │                                            │
│                   ┌────────┴─────���──┐                                   │
│                   │ PROCESS LOGIC   │                                   │
│                   │ ┌─────────────┐ │                                   │
│                   │ │ 1. Extract  │ │  14:35:47 → 14:00:00             │
│                   │ │    Hour     │ │                                   │
│                   │ ├─────────────┤ │                                   │
│                   │ │ 2. Load     │ │  Fetch from DB                    │
│                   │ │    Entity   │ │  or Create NEW                    │
│                   │ ├─────────────┤ │                                   │
│                   │ │ 3. Logic    │ │  IF PRODUCER:                     │
│                   │ │    Energy   │ │    produced += kwh                │
│                   │ │            │ │  IF USER:                         │
│                   │ │            │ │    available?                      │
│                   │ │            │ │    ├─ YES: used += kwh             │
│                   │ │            │ │    └─ NO: split community/grid     │
│                   │ ├─────────────┤ │                                   │
│                   │ │ 4. Save     │ │  UPDATE energy_usage              │
│                   │ │    to DB    │ │                                   │
│                   │ ├────���────────┤ │                                   │
│                   │ │ 5. Publish  │ │  Notify Percentage                │
│                   │ │    Update   │ │  Service                          │
│                   │ └─────────────┘ │                                   │
│                   └────────┬────────┘                                   │
│                            │                                            │
│            ┌───────────────┼───────────────┐                            │
│            ▼               ▼               ▼                            │
│     [PostgreSQL]  [RabbitMQ Update]  [Logging]                         │
│     energy_usage  energy.updates     Console Output                    │
│                   Queue                                                │
│                                                                         │
│     Database                                        Next Stage         │
│     ┌──────────────────────────────────┐                              │
│     │ hour │ produced │ used │ grid    │                              │
│     ├──────┼──────────┼──────┼─────────┤                              │
│     │14:00 │ 18.05    │ 17.50│ 1.95    │  → [Current % Service]       │
│     │13:00 │ 15.015   │14.033�� 2.049   │     Calculates Percentages   │
│     └──────┴──────────┴──────┴─────────┘                              │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2️⃣ Kritische Business Logic - Visualisiert

### PRODUKTION = 18 kWh | VERBRAUCH = 15 kWh | Grid = 2 kWh

```
┌──────────────────────────────────────────────────────────────┐
│ STUNDE 14:00-15:00                                           │
│ ════════════════════════════════════════════════════════════│
│                                                              │
│ Community Production Bucket:                                 │
│ ════════════════════════════════════════════════════════════│
│                                                              │
│ [████████████████████] = 18.0 kWh total produced             │
│                                                              │
│ Demand Scenarios:                                            │
│                                                              │
│ CASE 1: User wants 4 kWh (small load)                        │
│ ────────────────────────────────────────                     │
│ [████████████████████] 18 kWh available                      │
│         [════════════] 4 kWh needed                          │
│ Result: ✅ From Community! (0% from grid)                    │
│                                                              │
│ CASE 2: User wants 6 kWh (large load)                        │
│ ────────────────────────────────────────                     │
│ [████████████████████] 18 kWh available                      │
│       [██════════════] 6 kWh needed                          │
│       [██] = 4 from community  [====] = 2 from GRID ⚠️       │
│ Result: Split! Partial grid usage                           │
│                                                              │
│ CASE 3: High demand multiple users                           │
│ ────────��───────────────────────────────                     │
│ [████████████████████] 18 kWh available                      │
│ [██████████���████��████] 18 kWh used from community            │
│ [████████████████] 5 kWh still needed!                       │
│                  └─→ MUST come from GRID (expensive)        │
│ Result: Community depleted! Grid helps                       │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 3️⃣ Data Flow - Timeline View

```
TIME AXIS: 14:35:12 → 14:35:47 (35 Sekunden)

14:35:12  PRODUCER Message arrives
│         kwh = +0.025
│         ↓
│         Usage Service processes
│         ├─ Hour = 14:00:00
│         ├─ DB: CREATE new row
│         ├─ produced = 0.0 + 0.025 = 0.025 ✅
│         ├─ SAVE to DB
│         └─ PUBLISH UPDATE message
│
│ DB STATE: {14:00: produced=0.025, used=0.0, grid=0.0}
│
├─────────────────────────────────────────────
│
14:35:18  USER Message arrives
│         kwh = -0.015
│         ↓
│         Usage Service processes
│         ├─ Hour = 14:00:00
│         ├─ DB: LOAD existing row
│         ├─ available = 0.025 - 0.0 = 0.025 ✅
│         ├─ 0.025 >= 0.015? YES!
│         ├─ used = 0.0 + 0.015 = 0.015 ✅
│         ├─ grid stays 0.0
│         ├─ SAVE to DB
│         └─ PUBLISH UPDATE message
│
│ DB STATE: {14:00: produced=0.025, used=0.015, grid=0.0}
│
├───────────────────────────────────────────��─
│
14:35:27  USER Message arrives (POWER PEAK!)
│         kwh = -0.035
│         ↓
│         Usage Service processes
│         ├─ Hour = 14:00:00
│         ├─ DB: LOAD existing row
│         ├─ available = 0.025 - 0.015 = 0.010 ⚠️ NOT ENOUGH!
│         ├─ 0.010 >= 0.035? NO!
│         ├─ used = 0.015 + 0.010 = 0.025 (MAX reached!)
│         ├─ grid = 0.0 + (0.035-0.010) = 0.025 (GRID NEEDED!)
│         ├─ SAVE to DB
│         └─ PUBLISH UPDATE message
│
│ DB STATE: {14:00: produced=0.025, used=0.025, grid=0.025}
│           ^                        ^                    ^
│           Community MAXED!         Community MAXED!    GRID IN USE!
│
├─────────────────────────────────────────────
│
14:35:42  PRODUCER Message arrives (RESCUE!)
│         kwh = +0.05
│         ↓
│         Usage Service processes
│         ├─ Hour = 14:00:00
│         ├─ DB: LOAD existing row
│         ├─ produced = 0.025 + 0.05 = 0.075 (MUCH MORE! ☀️)
│         ├─ used stays 0.025
│         ├─ grid stays 0.025 (history, can't undo)
│         ├─ SAVE to DB
│         └─ PUBLISH UPDATE message
│
│ DB STATE: {14:00: produced=0.075, used=0.025, grid=0.025}
│           ^
│           Now plenty available (0.05 kWh spare)
│           but grid was already needed earlier
│
└───────────────────────────────��─────────────

FINAL METRICS FOR HOUR 14:00-15:00:
────────────────────────────────────
Total Produced:     0.075 kWh (3x solar panels)
Total Consumed:     0.025 kWh (from community)
Grid Needed:        0.025 kWh (expensive!)

Percentages (for Current Percentage Service):
  community_depleted = (0.025 / 0.075) × 100 = 33.3%
  grid_portion = (0.025 / (0.025+0.025)) × 100 = 50.0%

🎯 Insight: Community could not fully self-supply.
           Had to rely on grid for 1/2 of consumed energy.
           More renewables needed? Or lower demand?
```

---

## 4️⃣ Invarianten (Rules that NEVER break)

```
MATHEMATICAL CONSTRAINTS:
══════════════���══════════════════════════════════════════════

Rule 1: Community Used ≤ Community Produced
─────────────────────────���───────────────────
  ALWAYS: communityUsed ≤ communityProduced
  
  ✅ VALID:    used=15, produced=18  (15 ≤ 18)
  ✅ VALID:    used=18, produced=18  (18 ≤ 18)  MAX CASE
  ❌ INVALID:  used=20, produced=18  (20 > 18)  IMPOSSIBLE!
  
  Why? Can't use more energy than produced in community!


Rule 2: All Energy Values ≥ 0
──────────────────────────────
  produced ≥ 0
  used ≥ 0
  grid ≥ 0
  
  ✅ VALID:    produced=18.05, used=15, grid=3.05
  ❌ INVALID:  grid=-1  (Negative grid doesn't make sense!)


Rule 3: Grid Usage = Overflow from Demand
──────────────────────────────────────────
  grid_used = max(0, total_demand - community_produced)
  
  Example:
    demand = 20 kWh
    produced = 18 kWh
    grid = max(0, 20-18) = 2 kWh
    
    demand = 15 kWh
    produced = 18 kWh
    grid = max(0, 15-18) = 0 kWh  (no grid needed)


Rule 4: Hour Timestamp Format
──────────────────────────────
  ALWAYS: YYYY-MM-DDTHH:00:00
  
  Examples:
    ✅ "2025-01-10T14:00:00"
    ✅ "2025-12-31T23:00:00"
    ❌ "2025-01-10T14:35:47"  (minutes/seconds!)
    ❌ "2025-01-10 14:00:00"   (wrong format)
    
  → Usage Service normalizes all datetimes at Line 25!


THESE RULES ENSURE:
═════════════════════════════════════════════════════════════
  ✅ Data Integrity (garbage in = garbage out prevented)
  ✅ Business Logic Correctness (energy never created)
  ✅ Auditability (can trace every kWh)
  ✅ Fairness (no user pays for unsupplied energy)
```

---

## 5️⃣ Deployment Checklist

```
Before Starting Usage Service:
═════════════════════════════════════════════════════════════

INFRASTRUCTURE:
  ☑️ PostgreSQL running on localhost:5432
     psql -h localhost -U disysuser -d energydb
  
  ☑️ RabbitMQ running on localhost:5672
     http://localhost:15672 (management UI)
  
  ☑️ energy_usage table created (or Hibernate auto-creates)
     SELECT * FROM energy_usage;

APPLICATION:
  ☑️ Maven dependencies resolved
     ./mvnw dependency:resolve
  
  ☑️ Code compiles without errors
     ./mvnw clean compile
  
  ☑️ application.properties configured
     ├─ RABBITMQ_HOST=localhost
     ├─ RABBITMQ_PORT=5672
     ├─ DB_URL=jdbc:postgresql://localhost:5432/energydb
     ├─ DB_USER=disysuser
     └─ DB_PASSWORD=disyspw

START USAGE SERVICE:
  ./mvnw spring-boot:run
  
  ✅ Expected output:
     [INFO] Started UsageServiceApplication
     [INFO] Listening to RabbitMQ on 'energy.messages'
     [INFO] Database connection established

VERIFY:
  ☑️ No errors in logs
  ☑️ DB table created with 0 rows (or existing data)
  ☑️ Service ready to receive messages
  ☑️ Can see producer/user messages arriving

START PRODUCER & USER:
  cd energy-producer && ./mvnw spring-boot:run &
  cd energy-user && ./mvnw spring-boot:run &

OBSERVE:
  ☑️ Usage Service logs messages being processed
  ☑️ DB rows being inserted/updated
  ☑️ No exceptions thrown

SUCCESS = Messages flowing end-to-end! 🎉
```

---

## 6️⃣ Error Scenarios & Debugging

```
PROBLEM: "No such table: energy_usage"
═════════════════════════════════════════════════════════════
Cause:   Hibernate couldn't create table
Solution:
  1. Check PostgreSQL is running
  2. Check DB name is 'energydb'
  3. Delete table manually if migrations failed
  4. Set spring.jpa.hibernate.ddl-auto=create
  5. Restart service
  After first run, set back to 'update'


PROBLEM: "Connection refused: 5432"
═════════════════════════════════════════════════════════════
Cause:   PostgreSQL not running
Solution:
  1. docker run -d postgres:alpine
  2. Check: psql -h localhost
  3. Wait 5 seconds for service startup


PROBLEM: Messages processed but DB not updated
═════════════════════════════════════════════════════════════
Cause:   Repository.save() might have exception
Solution:
  1. Check logs for SQLException
  2. Check DB permissions: 
     GRANT ALL ON TABLE energy_usage TO disysuser;
  3. Verify connection string
  4. Check DB disk space


PROBLEM: Service crashes with "ClassNotFoundException"
═════════════════════════════════════════════════════════════
Cause:   Missing dependency
Solution:
  ./mvnw clean dependency:resolve
  ./mvnw clean compile
  Check pom.xml has all required libraries


DEBUGGING:
  Add to MessageListener:
  
    System.out.println("DEBUG: hour = " + hourKey);
    System.out.println("DEBUG: available = " + available);
    System.out.println("DEBUG: saved = " + saved);
  
  Or use SLF4J:
  
    private static final Logger log = 
      LoggerFactory.getLogger(MessageListener.class);
    
    log.info("Processing message: {}", message.getType());
    log.debug("Available energy: {}", available);
```

---

*Diese visuelle Anleitung hilft beim Verständnis und beim Troubleshooting des Usage Service.*

