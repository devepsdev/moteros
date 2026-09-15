from pipeline.fetch import Page
from pipeline.run import run
from pipeline.state import State
from services.moteros_api import SubmitResult

SOURCE = {"nombre": "Rutas", "url": "https://ejemplo.test/rutas"}
RUTAS = [
    {"nombre": "Ruta A", "puntoInicio": "A", "puntoFin": "B", "lugaresDePaso": ["A", "M", "B"]},
    {"nombre": "Ruta B", "puntoInicio": "C", "puntoFin": "D", "lugaresDePaso": ["C", "E", "D"]},
]


class FakeFetcher:
    def __init__(self, text="página", allowed=True, error=None):
        self.text, self._allowed, self.error = text, allowed, error

    def allowed(self, url):
        return self._allowed

    def fetch(self, url):
        if self.error:
            raise self.error
        return Page(url=url, text=self.text, content_hash=f"hash-{self.text}")


class FakeClient:
    def __init__(self, rutas):
        self.rutas = rutas
        self.calls = 0

    def complete_json(self, messages):
        self.calls += 1
        return {"rutas": self.rutas}


class FakeGeocoder:
    def locate(self, lugar, provincia):
        return (41.0, 2.0)


class FakeApi:
    def __init__(self, outcomes):
        self.outcomes = list(outcomes)
        self.sent = []

    def submit_suggestion(self, sugerencia):
        self.sent.append(sugerencia)
        return SubmitResult(self.outcomes.pop(0))


def _run(state, client, api, fetcher=None, **kwargs):
    return run([SOURCE], fetcher=fetcher or FakeFetcher(), client=client, geocoder=FakeGeocoder(), api=api, state=state, **kwargs)


def test_envia_las_rutas_con_puntos_y_cuenta_resultados():
    api = FakeApi(["created", "duplicate"])
    summary = _run(State(":memory:"), FakeClient(RUTAS), api)

    assert summary.created == 1
    assert summary.duplicates == 1
    assert [s["nombre"] for s in api.sent] == ["Ruta A", "Ruta B"]
    assert api.sent[0]["puntos"][0] == {"nombre": "A", "latitud": 41.0, "longitud": 2.0}


def test_una_pagina_sin_cambios_no_se_vuelve_a_analizar():
    state = State(":memory:")
    client = FakeClient(RUTAS)
    _run(state, client, FakeApi(["created", "created"]))
    summary = _run(state, client, FakeApi([]))

    assert client.calls == 1
    assert summary.skipped_unchanged == 1


def test_simulacion_no_envia_ni_marca_la_pagina():
    state = State(":memory:")
    client = FakeClient(RUTAS)
    _run(state, client, api=None, dry_run=True)
    _run(state, client, api=None, dry_run=True)

    assert client.calls == 2


def test_robots_y_errores_no_paran_la_pasada():
    blocked = _run(State(":memory:"), FakeClient(RUTAS), FakeApi([]), fetcher=FakeFetcher(allowed=False))
    assert blocked.skipped_robots == 1

    failed = _run(State(":memory:"), FakeClient(RUTAS), FakeApi([]), fetcher=FakeFetcher(error=RuntimeError("caída")))
    assert failed.failed_sources == ["Rutas"]
