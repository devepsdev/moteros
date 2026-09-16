from pipeline.fetch import html_to_text, track_links


def test_track_links_detecta_gpx_kml_y_wikiloc_por_texto_o_direccion():
    html = """
    <p>Ruta por Les Guilleries</p>
    <a href="https://drive.google.com/file/d/abc/view">Descargar archivo GPX</a>
    <a href="https://drive.google.com/drive/folders/xyz">Descargar Archivo KML</a>
    <a href="https://ejemplo.test/track.gpx?v=2">Track</a>
    <a href="https://www.wikiloc.com/rutas-moto/123">Ver en Wikiloc</a>
    <a href="https://drive.google.com/file/d/abc/view">Descargar archivo GPX</a>
    <a href="/relativo.gpx">GPX</a>
    <a href="https://ejemplo.test/gpxs-y-mas">Otra cosa</a>
    <a href="https://ejemplo.test/contacto">Contacto</a>
    """
    assert track_links(html) == [
        {"texto": "Descargar archivo GPX", "url": "https://drive.google.com/file/d/abc/view"},
        {"texto": "Descargar Archivo KML", "url": "https://drive.google.com/drive/folders/xyz"},
        {"texto": "Track", "url": "https://ejemplo.test/track.gpx?v=2"},
        {"texto": "Ver en Wikiloc", "url": "https://www.wikiloc.com/rutas-moto/123"},
    ]


def test_track_links_sin_enlaces_devuelve_lista_vacia():
    assert track_links("<p>Sin recorrido</p><a href='https://x.test'>Inicio</a>") == []


def test_html_to_text_quita_menus_y_scripts():
    texto = html_to_text("<nav>Menú</nav><main><h1>Ruta</h1><p>Sale de <b>Vic</b> , pasa por Seva.</p></main><script>x()</script>")
    assert texto == "Ruta\nSale de Vic, pasa por Seva."
