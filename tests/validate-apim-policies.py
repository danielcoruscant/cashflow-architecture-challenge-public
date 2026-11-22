from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
POLICIES = { # type: ignore
    "lancamentos": ("lancamentos.read", "lancamentos.write"),
    "consolidado": ("consolidado.read",),
}

for api, scopes in POLICIES.items(): # type: ignore
    path = ROOT / "infra/modules/apim/policies" / f"{api}.xml"
    try:
        policy = ET.parse(path).getroot()
    except (OSError, ET.ParseError) as error:
        raise SystemExit(f"{path}: não foi possível ler o XML: {error}") from error

    inbound = policy.find("./inbound")
    if inbound is None:
        raise SystemExit(f"{path}: bloco inbound ausente")

    elements = list(inbound)
    jwt_index = next((i for i, element in enumerate(elements) if element.tag == "validate-jwt"), None)
    limit_index = next((i for i, element in enumerate(elements) if element.tag == "rate-limit-by-key"), None)
    if jwt_index is None or limit_index is None or jwt_index >= limit_index:
        raise SystemExit(f"{path}: JWT deve ser validado antes do rate limit")

    rate_limit = elements[limit_index]
    if rate_limit.get("calls") != "120" or rate_limit.get("renewal-period") != "60":
        raise SystemExit(f"{path}: limite esperado de 120 chamadas por 60 segundos")
    counter_key = rate_limit.get("counter-key", "")
    if "oid" not in counter_key or "sub" not in counter_key:
        raise SystemExit(f"{path}: counter-key deve usar oid com fallback para sub")

    serialized = ET.tostring(policy, encoding="unicode")
    missing_scopes = [scope for scope in scopes if scope not in serialized] # pyright: ignore[reportUnknownVariableType]
    if missing_scopes:
        raise SystemExit(f"{path}: escopos ausentes: {', '.join(missing_scopes)}") # pyright: ignore[reportUnknownArgumentType]

    print(f"{path.relative_to(ROOT)}: política XML validada")
