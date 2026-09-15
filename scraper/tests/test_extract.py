from pipeline.extract import extract_routes, normalize_route

SOURCE = {"nombre": "Burgos", "url": "https://ejemplo.test/ruta", "provincia": "Burgos"}
SIN_PROVINCIA = {"nombre": "Listado", "url": "https://ejemplo.test/listado"}


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
        "lugaresDePaso": [
            {"nombre": "Caleruega", "provincia": "Burgos"},
            {"nombre": "Roa", "provincia": None},
            {"nombre": "Roa", "provincia": "Burgos"},
            {"nombre": "La Vid", "provincia": "Burgos"},
            {"nombre": "Peñalba de Castro", "provincia": "Burgos"},
        ],
        "distanciaKm": "175 km",
        "duracionMin": 240,
        "dificultad": "Moderada",
        "tipoTerreno": "Asfalto",
        "descripcion": "Ruta entre viñedos.",
    }, SOURCE)

    assert [l["nombre"] for l in ruta["lugares"]] == ["Caleruega", "Roa", "La Vid", "Peñalba de Castro"]
    # Sin provincia en un lugar se usa la de la fuente (una página de una sola provincia).
    assert ruta["lugares"][1]["provincia"] == "Burgos"
    assert ruta["nombre"] == "De Roma al vino"
    assert ruta["distanciaKm"] == 175.0
    assert ruta["duracionEstimadaMin"] == 240
    assert ruta["dificultad"] == "moderada"
    assert ruta["tipoTerreno"] == "asfalto"
    assert ruta["urlFuente"] == SOURCE["url"]


def test_cada_lugar_conserva_su_provincia_en_rutas_entre_provincias():
    ruta = normalize_route({
        "nombre": "Jaén y Almería", "puntoInicio": "Úbeda", "puntoFin": "Almería",
        "lugaresDePaso": [{"nombre": "Úbeda", "provincia": "Jaén"}, {"nombre": "Baza", "provincia": "Granada"},
                          {"nombre": "Almería", "provincia": "Almería"}],
    }, SIN_PROVINCIA)
    assert [(l["nombre"], l["provincia"]) for l in ruta["lugares"]] == [
        ("Úbeda", "Jaén"), ("Baza", "Granada"), ("Almería", "Almería")]


def test_la_salida_y_la_llegada_abren_y_cierran_el_recorrido():
    ruta = normalize_route({"nombre": "X", "puntoInicio": "A", "puntoFin": "B", "lugaresDePaso": ["M"]}, SOURCE)
    assert [l["nombre"] for l in ruta["lugares"]] == ["A", "M", "B"]


def test_descarta_rutas_sin_recorrido_que_dibujar():
    # Solo salida y llegada (p. ej. "Asturias → Andalucía") o circular a un único sitio.
    assert normalize_route({"nombre": "X", "puntoInicio": "Asturias", "puntoFin": "Andalucía"}, SIN_PROVINCIA) is None
    assert normalize_route({"nombre": "X", "puntoInicio": "A", "puntoFin": "A",
                            "lugaresDePaso": ["A", "Lagos", "A"]}, SIN_PROVINCIA) is None


def test_descarta_rutas_sin_salida_o_llegada_y_valores_no_permitidos():
    assert normalize_route({"nombre": "X", "puntoInicio": "A"}, SOURCE) is None
    assert normalize_route("no es un objeto", SOURCE) is None

    ruta = normalize_route({"nombre": "X", "puntoInicio": "A", "puntoFin": "B", "lugaresDePaso": ["M"],
                            "dificultad": "imposible", "tipoTerreno": "off-road", "distanciaKm": -3}, SOURCE)
    assert ruta["dificultad"] is None
    assert ruta["tipoTerreno"] == "offroad"
    assert ruta["distanciaKm"] is None


def test_extract_routes_ignora_respuestas_mal_formadas():
    assert extract_routes("texto", SOURCE, FakeClient({"otra": []})) == []

    client = FakeClient({"rutas": [{"nombre": "R", "puntoInicio": "A", "puntoFin": "B", "lugaresDePaso": ["M"]},
                                   {"nombre": "sin datos"}]})
    rutas = extract_routes("texto", SOURCE, client)
    assert [r["nombre"] for r in rutas] == ["R"]
    assert "Burgos" in client.messages[1]["content"]
