"""Resume JaCoCo e aplica o gate P9 sem descartar código de produção."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
MODULES = (
    "services/lancamentos-service",
    "services/consolidado-service",
    "integrations/cdc-publisher/function/cdc-function",
    "integrations/cdc-consumer",
)


def rate(covered, missed):
    return 100 * covered / (covered + missed) if covered + missed else None


passed = True
for module in MODULES:
    directory = ROOT / module
    report = ET.parse(directory / "target/site/jacoco/jacoco.xml").getroot()
    handwritten = {"LINE": [0, 0], "BRANCH": [0, 0]}
    generated = set()
    for package in report.findall("package"):
        for cls in package.findall("class"):
            source = Path(package.get("name")) / cls.get("sourcefilename")
            main_source = directory / "src/main/java" / source
            generated_source = directory / "target/generated-sources/openapi/src/main/java" / source
            if not main_source.is_file() and generated_source.is_file():
                generated.add(str(source))
                continue
            # Desconhecido não é excluído; código de produção continua no denominador.
            for kind in handwritten:
                counter = cls.find(f"./counter[@type='{kind}']")
                if counter is not None:
                    handwritten[kind][0] += int(counter.get("covered"))
                    handwritten[kind][1] += int(counter.get("missed"))

    print(f"\n{module}")
    for kind in handwritten:
        counter = report.find(f"./counter[@type='{kind}']")
        all_rate = rate(int(counter.get("covered")), int(counter.get("missed")))
        source_rate = rate(*handwritten[kind])
        print(f"  {kind}: relatório integral {all_rate:.2f}%; sem código gerado {source_rate:.2f}%")
        if source_rate < 80:
            passed = False
    print(f"  Arquivos gerados excluídos da segunda métrica: {len(generated)}")
    for source in sorted(generated):
        print(f"    {source}")

print("\nGate P9 Java (linhas e branches por módulo):", "aprovado" if passed else "não atingido")
sys.exit(0 if passed else 1)
