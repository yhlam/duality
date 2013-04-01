// $Revision$
// $Date$

INSERT INTO ofVersion (name, version) VALUES ('duality', 2);


CREATE TABLE duality
(
	id					INT 		IDENTITY,
	sender				VARCHAR		NOT NULL,
	receiver			VARCHAR		NOT NULL,
	time				BIGINT		NOT NULL,
	message				VARCHAR		NOT NULL,
	sender_latitude		DOUBLE,
	sender_longitude	DOUBLE,
	receiver_latitude	DOUBLE,
	receiver_longitude	DOUBLE
);
