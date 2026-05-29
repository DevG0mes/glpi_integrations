# Planilhas modelo (sync GLPI)

Arquivos para importar via `POST /api/sync/{starlink|chip|celular}` (campo `file`, CSV ou XLSX).

## Arquivos

| Arquivo | Uso |
|---------|-----|
| `template_starlink.csv` | Starlink (abre no Excel; separador `;`) |
| `template_chip.csv` | Chip |
| `template_celular.csv` | Celular |
| `templates_sincronizacao_ativos.xlsx` | Pasta de trabalho com 3 abas (gerar com Maven, abaixo) |
| `template_starlink.xlsx` | Somente Starlink (gerar com Maven) |
| `template_chip.xlsx` | Somente Chip (gerar com Maven) |
| `template_celular.xlsx` | Somente Celular (gerar com Maven) |

## Colunas

### Starlink

`id_ativo`, `nome`, `projeto`, `responsavel`, `email`, `senha_conta`, `senha_roteador`, `localidade`

### Chip

`id_ativo`, `iccid`, `numero`, `responsavel`, `status`

### Celular

`id_ativo`, `imei`, `modelo`, `responsavel`, `nome`

Linha 2 = exemplo opcional — substitua pelos seus dados ou apague após copiar o cabeçalho.

## Regenerar os XLSX (Java / POI, sem Python)

Na raiz do projeto:

```bash
mvn -Dtest=SpreadsheetTemplatesGeneratorTest test
```

Isso recria os arquivos nesta pasta.
