from pipeline.extract import extract_routes, normalize_route

SOURCE = {"nombre": "Burgos", "url": "https://ejemplo.test/ruta", "provincia": "Burgos"}


class FakeClient:
    def __init__(self, data):
        self.data = data
        self.messages = None

    def complete_json(self, messages):
        self.messages = messages
        return self.data


def test_normaliza_una_ruta_completa():
    ruta = normalize_route({
        "nombre": "  De Roma   al vino ",
        "puntoInicio": "Caleruega",
        "puntoFin": "Peñalba de Castro",
        "lugaresDePaso": ["Caleruega", "Roa", "Roa", "La Vid", "Peñalba de Castro"],
        "distanciaKm": "175 km",
        "duracionMin": 240,
        "dificultad": "Moderada",
        "tipoTerreno": "Asfalto",
        "descripcion": "Ruta entre viñedos.",
    }, SOURCE)

    assert ruta["nombre"] == "De Roma al vino"
    assert ruta["lugares"] == ["Caleruega", "Roa", "La Vid", "Peñalba de Castro"]
    assert ruta["distanciaKm"] == 175.0
    assert ruta["duracionEstimadaMin"] == 240
    assert ruta["dificultad"] == "moderada"
    assert ruta["tipoTerreno"] == "asfalto"
    # Sin provincia en la respuesta se usa la de la fuente, para geolocalizar mejor.
    assert ruta["provincia"] == "Burgos"
    assert ruta["urlFuente"] == SOURCE["url"]


def test_la_salida_y_la_llegada_abren_y_cierran_el_recorrido():
    ruta = normalize_route({"nombre": "X", "puntoInicio": "A", "puntoFin": "B", "lugaresDePaso": ["M"]}, SOURCE)
    assert ruta["lugares"] == ["A", "M", "B"]

    circular = normalize_route({"nombre": "X", "puntoInicio": "A", "puntoFin": "A", "lugaresDePaso": []}, SOURCE)
    assert circular["lugares"] == ["A", "A"]


def test_descarta_rutas_sin_salida_o_llegada_y_valores_no_permitidos():
    assert normalize_route({"nombre": "X", "puntoInicio": "A"}, SOURCE) is None
    assert normalize_route("no es un objeto", SOURCE) is None

    ruta = normalize_route({"nombre": "X", "puntoInicio": "A", "puntoFin": "B",
                            "dificultad": "imposible", "tipoTerreno": "off-road", "distanciaKm": -3}, SOURCE)
    assert ruta["dificultad"] is None
    assert ruta["tipoTerreno"] == "offroad"
    assert ruta["distanciaKm"] is None


def test_extract_routes_ignora_respuestas_mal_formadas():
    assert extract_routes("texto", SOURCE, FakeClient({"otra": []})) == []

    client = FakeClient({"rutas": [{"nombre": "R", "puntoInicio": "A", "puntoFin": "B"}, {"nombre": "sin datos"}]})
    rutas = extract_routes("texto", SOURCE, client)
    assert [r["nombre"] for r in rutas] == ["R"]
    assert "Burgos" in client.messages[1]["content"]
