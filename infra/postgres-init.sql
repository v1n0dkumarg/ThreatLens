\getenv app_password THREATLENS_DB_PASSWORD
CREATE ROLE threatlens_app LOGIN PASSWORD :'app_password';
ALTER DATABASE threatlens OWNER TO threatlens_app;