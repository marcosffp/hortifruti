@echo off
echo Formatando o código usando o Google Java Style...
mvn spotless:apply
IF %ERRORLEVEL% EQU 0 (
    echo Código formatado com sucesso!
) ELSE (
    echo Ocorreu um erro ao formatar o código.
)