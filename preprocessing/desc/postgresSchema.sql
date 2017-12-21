CREATE TABLE co_n (
    w1_id bigint NOT NULL,
    w2_id bigint NOT NULL,
    freq integer,
    sig double precision
);



CREATE TABLE co_s (
    w1_id bigint NOT NULL,
    w2_id bigint NOT NULL,
    freq integer,
    sig double precision
);


CREATE TABLE document (
    id bigint NOT NULL,
    content text,
    created date
);


CREATE TABLE documententity (
    docid bigint NOT NULL,
    entityid bigint NOT NULL,
    frequency bigint DEFAULT 1
);


CREATE TABLE duplicates (
    duplicateid bigint NOT NULL,
    focalid bigint NOT NULL
);

CREATE TABLE entity (
    id SERIAL PRIMARY KEY NOT NULL,
    name character varying,
    type character varying,
    frequency integer,
    isblacklisted boolean DEFAULT false NOT NULL
);


CREATE TABLE entityoffset (
    docid bigint NOT NULL,
    entid bigint NOT NULL,
    entitystart integer,
    entityend integer
);

CREATE TABLE eventtime (
    docid bigint,
    beginoffset integer,
    endoffset integer,
    timex character varying,
    type character varying,
    timexvalue character varying
);


CREATE TABLE inv_so (
    so_id bigint NOT NULL,
    s_id bigint NOT NULL
);

CREATE TABLE inv_w (
    w_id bigint,
    s_id bigint,
    pos integer
);



CREATE SEQUENCE labels_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE labels (
    id bigint DEFAULT nextval('labels_id_seq'::regclass) NOT NULL,
    label character varying(255) NOT NULL
);



CREATE TABLE metadata (
    docid bigint,
    key character varying,
    value text,
    type character varying
);


CREATE TABLE relationship (
    id bigint NOT NULL,
    entity1 bigint NOT NULL,
    entity2 bigint NOT NULL,
    frequency integer
);


CREATE TABLE sentences (
    s_id bigint NOT NULL,
    sentence text
);


CREATE TABLE sources (
    source character varying,
    date date NOT NULL,
    so_id bigint NOT NULL
);


CREATE SEQUENCE tags_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE tags (
    id bigint DEFAULT nextval('tags_id_seq'::regclass) NOT NULL,
    documentid bigint NOT NULL,
    labelid bigint NOT NULL
);

CREATE TABLE terms (
    docid bigint,
    term character varying,
    frequency integer
);



COMMENT ON TABLE terms IS 'Important terms extracted from the document';


CREATE TABLE words (
    w_id bigint NOT NULL,
    word character varying NOT NULL,
    freq integer
);

