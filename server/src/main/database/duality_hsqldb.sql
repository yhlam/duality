// $Revision$
// $Date$

INSERT INTO ofVersion (name, version) VALUES ('duality', 1);


CREATE TABLE duality
(
id BIGINT,
sender VARCHAR,
receiver VARCHAR,
time VARCHAR,
message VARCHAR,
sender_latitude DOUBLE,
sender_longitude DOUBLE,
receiver_latitude DOUBLE,
receiver_longitude DOUBLE
);
