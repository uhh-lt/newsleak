-- create tag table
CREATE TABLE tag(
	id integer not null,
	name text not null,
	PRIMARY KEY(id)
);

-- create tag-document table (relation between tag and document)
CREATE TABLE tagdocument(
	tagid integer not null,
	docid integer not null,
	PRIMARY KEY(tagid,docid)
);