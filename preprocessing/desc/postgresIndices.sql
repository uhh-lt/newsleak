ALTER TABLE ONLY document
    ADD CONSTRAINT document_pkey PRIMARY KEY (id);


ALTER TABLE ONLY labels
    ADD CONSTRAINT labels_pkey PRIMARY KEY (id);


ALTER TABLE ONLY tags
    ADD CONSTRAINT tags_pkey PRIMARY KEY (id);
    

CREATE INDEX "Inv_so_s_id_idx" ON inv_so USING btree (s_id);


CREATE INDEX "Inv_so_so_id_idx" ON inv_so USING btree (so_id);


CREATE INDEX "Inv_w_s_id_idx" ON inv_w USING btree (s_id);


CREATE INDEX "Inv_w_w_id_idx" ON inv_w USING btree (w_id);



CREATE INDEX "Metadata_DocId_idx" ON metadata USING btree (docid);

CREATE INDEX "Metadata_Key_idx" ON metadata USING btree (key);


CREATE INDEX "Metadata_Value_md5_idx" ON metadata USING btree (md5(value));


CREATE INDEX co_n_w1_id_idx ON co_n USING btree (w1_id);


CREATE INDEX co_n_w1_id_w2_id_idx ON co_n USING btree (w1_id, w2_id);


CREATE INDEX co_n_w2_id_idx ON co_n USING btree (w2_id);


CREATE INDEX co_n_w2_id_sig_idx ON co_n USING btree (w2_id, sig);


CREATE INDEX co_s_w1_id_idx ON co_s USING btree (w1_id);


CREATE INDEX co_s_w1_id_sig_idx ON co_s USING btree (w1_id, sig);



CREATE INDEX co_s_w1_id_w2_id_idx ON co_s USING btree (w1_id, w2_id);


CREATE INDEX co_s_w2_id_sig_idx ON co_s USING btree (w2_id, sig);


CREATE INDEX document_id_idx ON document USING btree (id);


CREATE INDEX documententity_docid_entityid_idx ON documententity USING btree (docid, entityid);


CREATE INDEX documententity_docid_idx ON documententity USING btree (docid);


CREATE INDEX documententity_entityid_idx ON documententity USING btree (entityid);


CREATE INDEX entityoffset_docid_entid_idx ON entityoffset USING btree (docid, entid);


CREATE INDEX eventtime_docid_idx ON eventtime USING btree (docid);


CREATE INDEX eventtime_docid_timex_type_timexvalue_idx ON eventtime USING btree (docid, timex, type, timexvalue);


CREATE INDEX eventtime_timex_idx ON eventtime USING btree (timex);



CREATE INDEX eventtime_timexvalue_idx ON eventtime USING btree (timexvalue);


CREATE INDEX relationship_entity1_entity2_idx ON relationship USING btree (entity1, entity2);


CREATE INDEX relationship_id_entity1_entity2_idx ON relationship USING btree (id, entity1, entity2);

CREATE INDEX relationship_id_entity1_idx ON relationship USING btree (id, entity1);



CREATE INDEX sentences_s_id_idx ON sentences USING btree (s_id);


CREATE INDEX terms_docid_idx ON terms USING btree (docid);


CREATE INDEX terms_docid_term_idx ON terms USING btree (docid, term);


CREATE INDEX terms_term_idx ON terms USING btree (term);


CREATE INDEX words_w_id_idx ON words USING btree (w_id);


CREATE INDEX words_word_idx ON words USING hash (word);


ALTER TABLE ONLY metadata
    ADD CONSTRAINT metadata_docid_fkey FOREIGN KEY (docid) REFERENCES document(id);


REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


REVOKE ALL ON TABLE co_n FROM PUBLIC;

REVOKE ALL ON TABLE co_s FROM PUBLIC;

REVOKE ALL ON TABLE document FROM PUBLIC;

REVOKE ALL ON TABLE documententity FROM PUBLIC;

REVOKE ALL ON TABLE entity FROM PUBLIC;


REVOKE ALL ON TABLE eventtime FROM PUBLIC;

REVOKE ALL ON TABLE inv_so FROM PUBLIC;
REVOKE ALL ON TABLE inv_w FROM PUBLIC;


REVOKE ALL ON TABLE labels FROM PUBLIC;

REVOKE ALL ON TABLE metadata FROM PUBLIC;


REVOKE ALL ON TABLE relationship FROM PUBLIC;


REVOKE ALL ON TABLE sentences FROM PUBLIC;


REVOKE ALL ON TABLE sources FROM PUBLIC;

REVOKE ALL ON TABLE tags FROM PUBLIC;

REVOKE ALL ON TABLE terms FROM PUBLIC;

REVOKE ALL ON TABLE words FROM PUBLIC;