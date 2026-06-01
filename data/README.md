# Pasta de planilhas (servidor)

Coloque aqui os arquivos CSV/XLSX para sync via API ou volume Docker (`./data` → `/data` no container).

Exemplo:

```bash
curl -X POST "http://localhost:8081/api/sync/starlink" \
  -F "file=@data/minha_planilha.csv"
```

Esta pasta está no `.gitignore` — não commite planilhas com dados reais.
