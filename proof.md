flowchart TD
%% Services
ing[IngestionServiceApp<br/>(port
7030)]
ward[WardServiceApp<br/>(port
7031)]

alert[AlertLevelServiceApp<br/>(port
7032)]

staff[StaffingServiceApp<br/>(port
7033)]
equip[EquipmentAlertServiceApp<br
/>(port 7034)]
broker[(ActiveMQ Broker)]

      %% Data
      csv[wards‑outdated.csv] --> ing

      %% HTTP (stage 2) – synchronous
calls
ward -->|GET /wards| ing
staff -->|GET /wards/{hubId}|
ward
staff -->|GET /alert-level| alert

      %% MQ (stage 3) – topic for
staffing events
staff -->|publish staffing
events| broker
broker -->|topic:
staffing-events-topic| ward
%% Ward subscribes to the topic
(stage 3)
ward -->|subscribes to topic|
broker

      %% MQ (stage 4) – queue for
equipment failures
ward -->|publish
equipment‑failure alerts| broker
broker -->|queue:
equipment-failure-queue| equip

      %% Styling (optional – just for
readability)
classDef service fill:#f9f,stroke
:#333,stroke-width:2px;
class ing,ward,alert,staff,equip
service;
classDef storage fill:#bbf,stroke
:#333,stroke-width:1px;
class csv storage;
classDef broker fill:#bfb,stroke:
#333,stroke-width:2px;
class broker broker;