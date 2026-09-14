from pipeline.geocode import Geocoder, add_coordinates
from pipeline.state import State


class FakeResponse:
    def __init__(self, data):
        self.data = data

    def raise_for_status(self):
        pass

    def json(self):
        return self.data


class FakeSession:
    """Devuelve coordenadas solo para las consultas que conoce."""

    def __init__(self, known):
        self.known = known
        self.headers = {}
        self.queries = []

    def get(self, url, params, timeout):
        self.queries.append(params["q"])
        coords = self.known.get(params["q"])
        return FakeResponse([{"lat": str(coords[0]), "lon": str(coords[1])}] if coords else [])


def test_cachea_resultados_y_tambien_los_no_encontrados():
    session = FakeSession({"Roa, Burgos": (41.696, -3.924)})
    geocoder = Geocoder(State(":memory:"), session=session, delay=0)

    assert geocoder.locate("Roa", "Burgos") == (41.696, -3.924)
    assert geocoder.locate("Roa", "Burgos") == (41.696, -3.924)
    assert geocoder.locate("Nolugar", "Burgos") is None
    assert geocoder.locate("Nolugar", "Burgos") is None

    # Roa una vez; Nolugar con provincia y sin ella, y nunca más.
    assert session.queries == ["Roa, Burgos", "Nolugar, Burgos", "Nolugar"]


def test_add_coordinates_convierte_lugares_en_puntos():
    session = FakeSession({"A, Burgos": (1.0, 2.0)})
    geocoder = Geocoder(State(":memory:"), session=session, delay=0)
    ruta = {"nombre": "R", "puntoInicio": "A", "puntoFin": "B", "provincia": "Burgos", "lugares": ["A", "B"], "urlFuente": "https://x"}

    sugerencia = add_coordinates(ruta, geocoder)

    assert "lugares" not in sugerencia and "provincia" not in sugerencia
    assert sugerencia["puntos"] == [
        {"nombre": "A", "latitud": 1.0, "longitud": 2.0},
        {"nombre": "B", "latitud": None, "longitud": None},
    ]
