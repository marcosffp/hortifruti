Claro! Vamos estruturar as informações de forma clara e padronizada, utilizando Markdown (MD), para facilitar a consulta e aplicação em planilhas ou sistemas de IA.

---

## 1. **Custo Operacional por Km (Fiorino)**

| Componente                         | Cálculo Estimado                             | Custo por KM (R\$) |
| ---------------------------------- | -------------------------------------------- | -----------------: |
| **Combustível**                    | R\$ 6,30 / 10 km/l                           |           **0,63** |
| **Óleo / Lubrificantes**           | Valor médio mantido                          |           **0,03** |
| **Pneus**                          | 1 jogo de 4 pneus (R\$ 1.600) ÷ 40.000 km    |           **0,04** |
| **Manutenção (revisões etc.)**     | Estimativa conservadora                      |           **0,15** |
| **Depreciação**                    | 15 % anual sobre R\$ 70 k, com 20.000 km/ano |           **0,53** |
| **Seguro + Licenciamento**         | R\$ 2.750/ano ÷ 20.000 km                    |           **0,14** |
| **Custo Operacional Total por KM** | Soma de todos os componentes                 |       **R\$ 1,52** |

---

## 2. **Custo de Tempo do Entregador (Simples Nacional)**

| Componente                         | Cálculo Estimado                                                                                                                                           | Custo por Hora (R\$) | Custo por Minuto (R\$) |
| ---------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------: | ---------------------: |
| **Salário Base**                   | Exemplo: R\$ 1.600/mês (192 h/mês)                                                                                                                         |                    — |                      — |
| **Encargos (Simples Nacional)**    | Férias (11,11 %) + 13º (8,33 %) + FGTS (8 %) + Multa FGTS (4 %) + Previ (7,93 %) ≈ **39,37 %** ([Treasy \| Planejamento e Controladoria][1], [Sólides][2]) |         + R\$ 628,00 |                      — |
| **Custo Total Mensal (CTM)**       | R\$ 1.600 + R\$ 628 = **R\$ 2.228**                                                                                                                        |                    — |                      — |
| **Custo por Hora**                 | CTM ÷ 192 horas                                                                                                                                            |      **R\$ 11,60/h** |                      — |
| **Custos Administrativos (\~15%)** | Gestão, app, comunicação, logística                                                                                                                        |         + R\$ 1,74/h |                      — |
| **Custo Hora Final**               | R\$ 11,60 + R\$ 1,74 = **R\$ 13,34/h**                                                                                                                     |                    — |                      — |
| **Custo por Minuto**               | R\$ 13,34 ÷ 60                                                                                                                                             |                    — |       **R\$ 0,22/min** |

---

## 3. **Margem de Lucro + Taxa Fixa (Frete)**

| Componente             | Cálculo Estimado                                                                |       Valor (R\$) / Percentual |
| ---------------------- | ------------------------------------------------------------------------------- | -----------------------------: |
| **Custo Total (CT)**   | Soma dos custos variáveis por entrega (tempo + km etc.)                         |   **Variável** (ex: R\$ 10,00) |
| **% de Margem (PM)**   | Para serviços: **aproximadamente 20 %** é a média de mercado ([Sebrae][3])      |                **0,20 (20 %)** |
| **Margem Monetária**   | CT × PM                                                                         |     Ex: CT = 10 → **R\$ 2,00** |
| **Taxa Fixa (TF)**     | Valor fixo por entrega para cobrir custos operacionais e investir em eficiência |          Exemplo: **R\$ 3,00** |
| **Margem Total (R\$)** | (CT × PM) + TF                                                                  | Ex: 2,00 + 3,00 = **R\$ 5,00** |

---

## Fórmula Base para Cálculo do Frete

```md
**Frete = (Custo Operacional por KM × Distância) + (Custo de Tempo × Tempo Estimado) + Margem de Lucro**
```

---

### Referências Utilizadas

* **Encargos no Simples Nacional**: Totalizando **39,37 %** sobre o salário, conforme fontes especializadas ([Treasy | Planejamento e Controladoria][1], [Sólides][2]).
* **Margem de serviços recomendada no mercado**: **20 %** sobre o total das vendas ([Sebrae][3]).

Se desejar, posso fornecer essas informações em formato de planilha ou adaptadas para uso em sistemas de IA. Basta solicitar!

[1]: https://www.treasy.com.br/blog/encargos-trabalhistas/?utm_source=chatgpt.com "Encargos trabalhistas: quanto custa um funcionário? [Tabela] - Treasy"
[2]: https://solides.com.br/blog/como-fazer-o-calculo-do-custo-do-funcionario/?utm_source=chatgpt.com "Como fazer o cálculo do custo do funcionário? Aprenda aqui! - Solides"
[3]: https://sebrae.com.br/sites/PortalSebrae/artigos/saiba-como-definir-sua-margem-de-lucro%2Ce361fcbf64756810VgnVCM1000004c00210aRCRD?utm_source=chatgpt.com "Saiba como definir sua margem de lucro - Sebrae"
