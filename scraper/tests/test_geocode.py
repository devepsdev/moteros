from pipeline.geocode import Geocoder, add_coordinates, descartar_atipicos
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
        self.params = params
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


def test_add_coordinates_busca_cada_lugar_con_su_provincia():
    session = FakeSession({"Úbeda, Jaén": (38.01, -3.37), "Baza, Granada": (37.49, -2.77)})
    geocoder = Geocoder(State(":memory:"), session=session, delay=0)
    ruta = {"nombre": "R", "puntoInicio": "Úbeda", "puntoFin": "Baza", "urlFuente": "https://x",
            "lugares": [{"nombre": "Úbeda", "provincia": "Jaén"}, {"nombre": "Baza", "provincia": "Granada"}]}

    sugerencia = add_coordinates(ruta, geocoder)

    assert "lugares" not in sugerencia
    assert session.queries == ["Úbeda, Jaén", "Baza, Granada"]
    assert sugerencia["puntos"] == [
        {"nombre": "Úbeda", "latitud": 38.01, "longitud": -3.37},
        {"nombre": "Baza", "latitud": 37.49, "longitud": -2.77},
    ]


def test_descarta_coordenadas_de_un_homonimo_muy_lejano():
    # Recorrido por Picos de Europa con un «Potes» situado por error en el oeste de Asturias.
    puntos = [
        {"nombre": "Cangas de Onís", "latitud": 43.35, "longitud": -5.13},
        {"nombre": "Riaño", "latitud": 42.97, "longitud": -5.02},
        {"nombre": "Potes", "latitud": 43.13, "longitud": -6.35},
        {"nombre": "Panes", "latitud": 43.32, "longitud": -4.58},
        {"nombre": "Unquera", "latitud": 43.37, "longitud": -4.51},
    ]
    descartar_atipicos(puntos)

    assert [p["nombre"] for p in puntos if p["latitud"] is None] == ["Potes"]


def test_descarta_un_extremo_que_cae_en_otra_region():
    # «Arles» encontrado en Castilla en vez de en la Catalunya Nord, al final del recorrido.
    puntos = [
        {"nombre": "Figueres", "latitud": 42.267, "longitud": 2.961},
        {"nombre": "La Jonquera", "latitud": 42.419, "longitud": 2.875},
        {"nombre": "Ceret", "latitud": 42.486, "longitud": 2.749},
        {"nombre": "Arles", "latitud": 40.95, "longitud": -4.12},
    ]
    descartar_atipicos(puntos)

    assert [p["nombre"] for p in puntos if p["latitud"] is None] == ["Arles"]


def test_no_toca_los_extremos_de_una_ruta_larga_y_lineal():
    # Vuelta por la Costa Daurada de Tortosa a Barcelona: salida y llegada lejos del centro, pero bien.
    puntos = [
        {"nombre": "Tortosa", "latitud": 40.812, "longitud": 0.521},
        {"nombre": "L'Ametlla de Mar", "latitud": 40.883, "longitud": 0.802},
        {"nombre": "Cambrils", "latitud": 41.067, "longitud": 1.059},
        {"nombre": "Tarragona", "latitud": 41.119, "longitud": 1.245},
        {"nombre": "El Vendrell", "latitud": 41.219, "longitud": 1.535},
        {"nombre": "Vilanova i la Geltrú", "latitud": 41.224, "longitud": 1.725},
        {"nombre": "Begues", "latitud": 41.330, "longitud": 1.931},
        {"nombre": "Gavà", "latitud": 41.305, "longitud": 2.001},
        {"nombre": "Barcelona", "latitud": 41.387, "longitud": 2.170},
    ]
    descartar_atipicos(puntos)
    assert all(p["latitud"] is not None for p in puntos)


def test_no_toca_recorridos_largos_pero_coherentes():
    # Una ruta Burgos → Almería es larga, pero sus puntos siguen una línea sin saltos raros.
    puntos = [{"nombre": str(i), "latitud": 42.3 - i * 0.5, "longitud": -3.7 + i * 0.15} for i in range(10)]
    descartar_atipicos(puntos)
    assert all(p["latitud"] is not None for p in puntos)


def test_busca_tambien_en_paises_vecinos():
    session = FakeSession({"Cotlliure, Pirineos Orientales": (42.525, 3.083)})
    geocoder = Geocoder(State(":memory:"), session=session, delay=0)
    assert geocoder.locate("Cotlliure", "Pirineos Orientales") == (42.525, 3.083)
    assert set(session.params["countrycodes"].split(",")) >= {"es", "fr", "ad"}
