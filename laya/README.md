# Laya local

Instalación recomendada para desarrollo:

```bash
python -m venv .venv
source .venv/bin/activate       # Linux/macOS
# .venv\\Scripts\\activate      # Windows PowerShell
pip install "laya[serve]"
```

Arranque en CPU, precargando el checkpoint multilingüe:

```bash
export LAYA_DEVICE=cpu
export LAYA_PRELOAD=1
export LAYA_MODELS=multilingual
laya-serve
```

En PowerShell:

```powershell
$env:LAYA_DEVICE="cpu"
$env:LAYA_PRELOAD="1"
$env:LAYA_MODELS="multilingual"
laya-serve
```

Healthcheck: `GET http://localhost:8000/health`.
El decision-engine consume `POST /v1/systemone`.
