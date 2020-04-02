@echo off
FOR /L %%G IN (1,1,22) DO (
	java -jar "hgtools.jar" -extract -sql -skip 0 2 dss.ddl tpch/query%%G.sql
	
	IF %ERRORLEVEL% NEQ 0 ( 
		ECHO "Error %%i"
	)
)
PAUSE