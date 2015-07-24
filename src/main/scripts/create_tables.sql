CREATE TABLE resource_table (
  uuid VARCHAR2(40) NOT NULL,
  json CLOB,
  key CLOB,
  key_id VARCHAR2(40) NOT NULL
);

CREATE TABLE patient_map (
  id VARCHAR2(40) NOT NULL,
  signature VARCHAR2(40) NOT NULL,
  subject_id VARCHAR2(40),
  start_date DATE,
  system VARCHAR2(200),
  code VARCHAR2(200),
  display VARCHAR2(200),
  version_id VARCHAR2(200)
);