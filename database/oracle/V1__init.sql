DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = 'TRANSLATE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
        CREATE TABLE translate
        (
            lang_key VARCHAR2(255) NOT NULL,
            lang     VARCHAR2(20)  NOT NULL,
            value    CLOB,
            CONSTRAINT pk_translate PRIMARY KEY (lang_key, lang)
        )';
    END IF;
END;
/
