#!/bin/bash
echo "Formatando o código usando o Google Java Style..."
./mvnw spotless:apply
if [ $? -eq 0 ]; then
    echo "Código formatado com sucesso!"
else
    echo "Ocorreu um erro ao formatar o código."
fi