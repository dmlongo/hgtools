@ECHO OFF
FOR /r %%i IN (output\tpch\*) DO (
	echo %%i
	java -jar "hgtools.jar" -convert -sql dss.ddl %%i
	
	IF %ERRORLEVEL% NEQ 0 ( 
		ECHO "Error %%i"
	)
)
PAUSE

Rem FOR /L %%G IN (1,1,22) DO java -jar "hgtools.jar" -extract -sql dss.ddl tpch/%%G.sql
Rem -convert -sql dss.ddl 5.sql