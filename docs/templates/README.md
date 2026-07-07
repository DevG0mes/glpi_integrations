# Planilhas modelo (sync GLPI)

Arquivos para importar via `POST /api/sync/{computers|starlink|chip|celular|garantia}` (campo `file`, CSV ou XLSX).

## Arquivos

| Arquivo | Uso |
|---------|-----|
| `template_computers.csv` | Computers |
| `template_starlink.csv` | Starlink (abre no Excel; separador `;`) |
| `template_chip.csv` | Chip |
| `template_celular.csv` | Celular |
| `template_garantia.csv` | Garantia |
| `templates_sincronizacao_ativos.xlsx` | Pasta de trabalho com 3 abas (gerar com Maven, abaixo) |
| `template_starlink.xlsx` | Somente Starlink (gerar com Maven) |
| `template_chip.xlsx` | Somente Chip (gerar com Maven) |
| `template_celular.xlsx` | Somente Celular (gerar com Maven) |

## Colunas

### Computers

`id_ativo`, `serial`, `responsavel`, `local`, `grupo`, `tipo`, `fabricante`, `nome`, `observacao`, `vencimento_garantia`, `cod_mega`

### Starlink

`id_ativo`, `nome`, `projeto`, `responsavel`, `email`, `senha_conta`, `senha_roteador`, `localidade`

### Chip

`id_ativo`, `iccid`, `numero`, `responsavel`, `status`, `vencimento`

### Celular

`id_ativo`, `imei`, `modelo`, `responsavel`, `nome`

### Garantia

`Nome`, `Status`, `Vencimento Garantia`, `Numero de Serie`, `Custo`, `NFS`, `Modelo Garantia`

Linha 2 = exemplo opcional — substitua pelos seus dados ou apague após copiar o cabeçalho.

## Regenerar os XLSX (Java / POI, sem Python)

Na raiz do projeto:

```bash
mvn -Dtest=SpreadsheetTemplatesGeneratorTest test
```

Isso recria os arquivos nesta pasta.
