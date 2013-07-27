package geoscript.layer

import geoscript.geom.*
import geoscript.layer.Band
import geoscript.layer.GeoTIFF
import geoscript.layer.Histogram
import geoscript.layer.Raster
import geoscript.proj.Projection
import geoscript.style.ColorMap
import geoscript.style.Symbolizer
import org.junit.Test
import javax.imageio.ImageIO
import static org.junit.Assert.*
import geoscript.layer.Layer
import geoscript.workspace.Memory
import geoscript.feature.Field

import static org.junit.Assert.assertEquals

/**
 * The Raster unit test
 */
class RasterTestCase {
	
    @Test void raster() {
        File file = new File(getClass().getClassLoader().getResource("alki.tif").toURI())
        assertNotNull(file)

        GeoTIFF geoTIFF = new GeoTIFF()
        Raster raster = geoTIFF.read(file)
        assertNotNull(raster)

        assertEquals("GeoTIFF", geoTIFF.name)
        assertEquals("EPSG:2927", raster.proj.id)

        assertNotNull raster.data
        assertNotNull raster.image

        Bounds bounds = raster.bounds
        assertEquals(1166191.0260847565, bounds.minX, 0.0000000001)
        assertEquals(1167331.8522748263, bounds.maxX, 0.0000000001)
        assertEquals(822960.0090852415, bounds.minY, 0.0000000001)
        assertEquals(824226.3820666744, bounds.maxY, 0.0000000001)
        assertEquals("EPSG:2927", bounds.proj.id)

        def (int w, int h) = raster.size
        assertEquals(761, w)
        assertEquals(844, h)

        List<Band> bands = raster.bands
        assertEquals(3, bands.size())
        assertEquals("RED_BAND", bands[0].toString())
        assertEquals("GREEN_BAND", bands[1].toString())
        assertEquals("BLUE_BAND", bands[2].toString())

        def (int bw, int bh) = raster.blockSize
        assertEquals(761, bw)
        assertEquals(3, bh)

        def (double pw, double ph) = raster.pixelSize
        assertEquals(1.4991145730220545, pw, 0.000000000001)
        assertEquals(1.5004419211290778, ph, 0.000000000001)
    }

    @Test void rasterFromImage() {
        File file = new File(getClass().getClassLoader().getResource("alki.tif").toURI())
        assertNotNull(file)
        def image = ImageIO.read(file)
        def bounds = new Bounds(1166191.0260847565,1167331.8522748263,822960.0090852415,824226.3820666744,"EPSG:2927")
        Raster raster = new Raster(image, bounds)
        assertNotNull raster
        assertNotNull raster.coverage
    }

    @Test void getColumnsAndRows() {
        Bounds bounds = new Bounds(7, 7, 17, 17, "EPSG:4326")
        List data = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Raster raster = new Raster(data, bounds)
        int cols = raster.cols
        int rows = raster.rows
        assertEquals(7, cols)
        assertEquals(5, rows)
    }

    @Test void crop() {
        File file = new File(getClass().getClassLoader().getResource("alki.tif").toURI())
        assertNotNull(file)

        GeoTIFF geoTIFF = new GeoTIFF()
        Raster raster = geoTIFF.read(file)
        assertNotNull(raster)

        Bounds bounds = new Bounds(1166191.0260847565, 822960.0090852415, 1166761.4391797914, 823593.1955759579)
        Raster cropped = raster.crop(bounds)
        assertNotNull(cropped)
        assertEquals(bounds.minX, cropped.bounds.minX, 1d)
        assertEquals(bounds.maxX, cropped.bounds.maxX, 1d)
        assertEquals(bounds.minY, cropped.bounds.minY, 1d)
        assertEquals(bounds.maxY, cropped.bounds.maxY, 1d)
    }

    @Test void cropWithPixels() {
        File file = new File(getClass().getClassLoader().getResource("alki.tif").toURI())
        assertNotNull(file)

        GeoTIFF geoTIFF = new GeoTIFF()
        Raster raster = geoTIFF.read(file)
        assertNotNull(raster)

        Raster cropped = raster.crop(0,0,10,10)
        assertNotNull(cropped)
        Bounds bounds = new Bounds(1166191.0260847565,824209.8772055419,1166207.5163450597,824226.3820666744,"EPSG:2927")
        assertEquals(bounds.minX, cropped.bounds.minX, 1d)
        assertEquals(bounds.maxX, cropped.bounds.maxX, 1d)
        assertEquals(bounds.minY, cropped.bounds.minY, 1d)
        assertEquals(bounds.maxY, cropped.bounds.maxY, 1d)
    }

    @Test void reproject() {
        File file = new File(getClass().getClassLoader().getResource("alki.tif").toURI())
        assertNotNull(file)

        GeoTIFF geoTIFF = new GeoTIFF()
        Raster raster = geoTIFF.read(file)
        assertNotNull(raster)
        assertEquals("EPSG:2927", raster.proj.id)

        Raster reprojected = raster.reproject(new Projection("EPSG:4326"))
        assertNotNull(reprojected)
        assertEquals("EPSG:4326", reprojected.proj.id)
    }

    @Test void reclassify() {
        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Raster raster1 = new Raster(data, bounds)
        Raster raster2 = raster1.reclassify([
            [min: 0, max: 2, value: 5],
            [min: 2, max: 3, value:  10]
        ])

        assertEquals 5, raster2.eval(new Point(0.5,0.5))[0], 0.1
        assertEquals 5, raster2.eval(new Point(1.5,1.5))[0], 0.1
        assertEquals 5, raster2.eval(new Point(2.5,2.5))[0], 0.1
        assertEquals 10, raster2.eval(new Point(3.5,2.5))[0], 0.1
    }

    @Test void write() {
        // Read a GeoTIFF
        File file = new File(getClass().getClassLoader().getResource("alki.tif").toURI())
        assertNotNull(file)
        GeoTIFF geoTIFF = new GeoTIFF()
        Raster raster = geoTIFF.read(file)
        assertNotNull(raster)
        assertEquals("EPSG:2927", raster.proj.id)

        // Reproject it to 4326
        Raster reprojected = raster.reproject(new Projection("EPSG:4326"))
        assertNotNull(reprojected)
        assertEquals("EPSG:4326", reprojected.proj.id)

        // Write the reprojected GeoTIFF to a file
        File file1 = File.createTempFile("reprojected_raster",".tif")
        println(file1)
        geoTIFF.write(reprojected, file1)

        // Read the written reprojected GeoTIFF
        Raster raster2 = geoTIFF.read(file1)
        assertNotNull(raster2)
        assertEquals("EPSG:4326", raster2.proj.id)
    }

    @Test void eval() {
        File file = new File(getClass().getClassLoader().getResource("alki.tif").toURI())
        assertNotNull(file)
        GeoTIFF geoTIFF = new GeoTIFF()
        Raster raster = geoTIFF.read(file)
        assertNotNull(raster)

        // eval with Point
        def value = raster.eval(new Point(1166761.4391797914, 823593.1955759579))
        assertEquals(226, value[0], 0.1)
        assertEquals(234, value[1], 0.1)
        assertEquals(210, value[2], 0.1)

        // eval with Pixel
        value = raster.eval(10,15)
        assertEquals(39, value[0], 0.1)
        assertEquals(83, value[1], 0.1)
        assertEquals(100, value[2], 0.1)

        // getAt with Point
        value = raster[new Point(1166761.4391797914, 823593.1955759579)]
        assertEquals(226, value[0], 0.1)
        assertEquals(234, value[1], 0.1)
        assertEquals(210, value[2], 0.1)

        // getAt with Pixel
        value = raster[[10,15]]
        assertEquals(39, value[0], 0.1)
        assertEquals(83, value[1], 0.1)
        assertEquals(100, value[2], 0.1)

        // getValue with Pixel
        assertEquals(39, raster.getValue([10,15], 0), 0.1)
        assertEquals(83, raster.getValue([10,15], 1), 0.1)
        assertEquals(100, raster.getValue([10,15], 2), 0.1)

        // getValue with Point
        assertEquals(226, raster.getValue(new Point(1166761.4391797914, 823593.1955759579), 0), 0.1)
        assertEquals(234, raster.getValue(new Point(1166761.4391797914, 823593.1955759579), 1), 0.1)
        assertEquals(210, raster.getValue(new Point(1166761.4391797914, 823593.1955759579), 2), 0.1)
    }

    @Test void getValueFromRaster() {
        File file = new File(getClass().getClassLoader().getResource("raster.tif").toURI())
        assertNotNull(file)
        GeoTIFF geoTIFF = new GeoTIFF()
        Raster raster = geoTIFF.read(file)
        assertNotNull(raster)

        // By default values are returned as doubles
        assertEquals(raster.eval(new Point(-179, 89))[0], 184, 0.1)
        assertEquals(raster.eval(new Point(0, 0))[0], 227, 0.1)
        assertEquals(raster.eval(1,1)[0], 184, 0.1)

        // Make sure explicit types also work (boolean won't work in this case)
        assertEquals(raster.eval(new Point(-179, 89), "int")[0], 184)
        assertEquals(raster.eval(new Point(-179, 89), "double")[0], 184.0, 0.1)
        assertEquals(raster.eval(new Point(-179, 89), "float")[0], 184.0, 0.1)
        assertEquals(raster.eval(new Point(-179, 89), "byte")[0], -72)
        assertEquals(raster.eval(new Point(-179, 89), "default")[0], -72)
    }

    @Test void setValue() {
        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Raster raster = new Raster(data, bounds)
        assertEquals 1, raster.eval([1,1])[0], 0.1
        raster.setValue([1,1],5)
        assertEquals 5, raster.eval([1,1])[0], 0.1

        assertEquals 3, raster[[3,2]][0], 0.1
        raster[[3,2]] = 10
        assertEquals 10, raster[[3,2]][0], 0.1
    }

    @Test void add() {
        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data1 = [
            [0,0,0,0,0,0,0],
            [0,1,1,1,1,1,0],
            [0,1,2,3,2,1,0],
            [0,1,1,1,1,1,0],
            [0,0,0,0,0,0,0]
        ]
        Raster raster1 = new Raster(data1, bounds)

        List data2 = [
            [1,1,1,1,1,1,1],
            [1,2,2,2,2,2,1],
            [1,2,3,4,3,2,1],
            [1,2,2,2,2,2,1],
            [1,1,1,1,1,1,1]
        ]
        Raster raster2 = new Raster(data2, bounds)

        Raster raster3 = raster1 + raster2
        assertEquals 1, raster3.eval(new Point(0.5,0.5))[0], 0.1
        assertEquals 3, raster3.eval(new Point(1.5,1.5))[0], 0.1
        assertEquals 5, raster3.eval(new Point(2.5,2.5))[0], 0.1
        assertEquals 7, raster3.eval(new Point(3.5,2.5))[0], 0.1
    }

    @Test void addConstant() {
        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data1 = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Raster raster1 = new Raster(data1, bounds)
        Raster raster2 = raster1 + 5.0
        assertEquals 5, raster2.eval(new Point(0.5,0.5))[0], 0.1
        assertEquals 6, raster2.eval(new Point(1.5,1.5))[0], 0.1
        assertEquals 7, raster2.eval(new Point(2.5,2.5))[0], 0.1
        assertEquals 8, raster2.eval(new Point(3.5,2.5))[0], 0.1
    }

    @Test void multiply() {
        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data1 = [
            [0,0,0,0,0,0,0],
            [0,1,1,1,1,1,0],
            [0,1,2,3,2,1,0],
            [0,1,1,1,1,1,0],
            [0,0,0,0,0,0,0]
        ]
        Raster raster1 = new Raster(data1, bounds)

        List data2 = [
            [1,1,1,1,1,1,1],
            [1,2,2,2,2,2,1],
            [1,2,3,4,3,2,1],
            [1,2,2,2,2,2,1],
            [1,1,1,1,1,1,1]
        ]
        Raster raster2 = new Raster(data2, bounds)

        Raster raster3 = raster1 * raster2
        assertEquals 0, raster3.eval(new Point(0.5,0.5))[0], 0.1
        assertEquals 2, raster3.eval(new Point(1.5,1.5))[0], 0.1
        assertEquals 6, raster3.eval(new Point(2.5,2.5))[0], 0.1
        assertEquals 12, raster3.eval(new Point(3.5,2.5))[0], 0.1
    }

    @Test void multiplyConstant() {
        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data1 = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Raster raster1 = new Raster(data1, bounds)
        Raster raster2 = raster1 * 5.0

        assertEquals 0, raster2.eval(new Point(0.5,0.5))[0], 0.1
        assertEquals 5, raster2.eval(new Point(1.5,1.5))[0], 0.1
        assertEquals 10, raster2.eval(new Point(2.5,2.5))[0], 0.1
        assertEquals 15, raster2.eval(new Point(3.5,2.5))[0], 0.1
    }

    @Test void divide() {

        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data1 = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Raster raster1 = new Raster(data1, bounds)

        List data2 = [
                [1,1,1,1,1,1,1],
                [1,2,2,2,2,2,1],
                [1,2,3,4,3,2,1],
                [1,2,2,2,2,2,1],
                [1,1,1,1,1,1,1]
        ]
        Raster raster2 = new Raster(data2, bounds)

        Raster raster3 = raster1 / raster2

        assertEquals 0, raster3.eval(new Point(0.5,0.5))[0], 0.1
        assertEquals 0.5, raster3.eval(new Point(1.5,1.5))[0], 0.1
        assertEquals 0.6667, raster3.eval(new Point(2.5,2.5))[0], 0.1
        assertEquals 0.75, raster3.eval(new Point(3.5,2.5))[0], 0.1
    }

    @Test void divideConstant() {
        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data1 = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Raster raster1 = new Raster(data1, bounds)
        Raster raster2 = raster1 / 1.2

        assertEquals 0, raster2.eval(new Point(0.5,0.5))[0], 0.1
        assertEquals 0.833, raster2.eval(new Point(1.5,1.5))[0], 0.1
        assertEquals 1.666, raster2.eval(new Point(2.5,2.5))[0], 0.1
        assertEquals 2.5, raster2.eval(new Point(3.5,2.5))[0], 0.1
    }

    @Test void minus() {

        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data1 = [
                [5,5,5,5,5,5,5],
                [5,6,6,6,6,6,5],
                [5,6,7,6,7,6,5],
                [5,6,6,6,6,6,5],
                [5,5,5,5,5,5,5]
        ]
        Raster raster1 = new Raster(data1, bounds)

        List data2 = [
                [1,1,1,1,1,1,1],
                [1,2,2,2,2,2,1],
                [1,2,3,4,3,2,1],
                [1,2,2,2,2,2,1],
                [1,1,1,1,1,1,1]
        ]
        Raster raster2 = new Raster(data2, bounds)

        Raster raster3 = raster1 - raster2

        assertEquals(4, raster3.eval(new Point(0.5,0.5))[0], 0.1)
        assertEquals(4, raster3.eval(new Point(1.5,1.5))[0], 0.1)
        assertEquals(4, raster3.eval(new Point(2.5,2.5))[0], 0.1)
        assertEquals(2, raster3.eval(new Point(3.5,2.5))[0], 0.1)
    }

    @Test void minusConstant() {
        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data1 = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Raster raster1 = new Raster(data1, bounds)
        Raster raster2 = raster1 - 0.5

        assertEquals(-0.5, raster2.eval(new Point(0.5,0.5))[0], 0.1)
        assertEquals 0.5, raster2.eval(new Point(1.5,1.5))[0], 0.1
        assertEquals 1.5, raster2.eval(new Point(2.5,2.5))[0], 0.1
        assertEquals 2.5, raster2.eval(new Point(3.5,2.5))[0], 0.1
    }

    @Test void minusFromConstant() {
        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data1 = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Raster raster1 = new Raster(data1, bounds)
        Raster raster2 = raster1.minusFrom(6)

        assertEquals 6, raster2.eval(new Point(0.5,0.5))[0], 0.1
        assertEquals 5, raster2.eval(new Point(1.5,1.5))[0], 0.1
        assertEquals 4, raster2.eval(new Point(2.5,2.5))[0], 0.1
        assertEquals 3, raster2.eval(new Point(3.5,2.5))[0], 0.1
    }

    @Test void contours() {
        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data = [
            [0,0,0,0,0,0,0],
            [0,1,1,1,1,1,0],
            [0,1,2,3,2,1,0],
            [0,1,1,1,1,1,0],
            [0,0,0,0,0,0,0]
        ]
        Raster raster = new Raster(data, bounds)
        Layer layer = raster.contours(0, 0.25, true, true, bounds)
        assertNotNull layer
        assertTrue layer.count > 0

        layer = raster.contours(0, [0.25,0.5,0.75], true, true, bounds)
        assertNotNull layer
        assertTrue layer.count > 0

        layer = raster.contours(0, [0.25,0.5,0.75], true, true)
        assertNotNull layer
        assertTrue layer.count > 0
    }

    @Test void getPolygonLayer() {
        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data = [
            [0,0,0,0,0,0,0],
            [0,1,1,1,1,1,0],
            [0,1,2,3,2,1,0],
            [0,1,1,1,1,1,0],
            [0,0,0,0,0,0,0]
        ]
        Raster raster = new Raster(data, bounds)
        Layer layer = raster.getPolygonLayer(band: 0, insideEdges: true, roi: bounds.geometry)
        assertNotNull layer
        assertTrue layer.count > 0

        layer = raster.getPolygonLayer(band: 0, insideEdges: true, roi: bounds.geometry, noData: [-1,0], range: [[min: 1, max: 3]])
        assertNotNull layer
        assertTrue layer.count > 0

        layer = raster.polygonLayer
        assertNotNull layer
        assertTrue layer.count > 0
    }

    @Test void getPointLayer() {
        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data = [
            [0,0,0,0,0,0,0],
            [0,1,1,1,1,1,0],
            [0,1,2,3,2,1,0],
            [0,1,1,1,1,1,0],
            [0,0,0,0,0,0,0]
        ]
        Raster raster = new Raster(data, bounds)
        Layer layer = raster.getPointLayer()
        assertNotNull layer
        assertTrue layer.count > 0
    }

    @Test void scale() {
        File file = new File(getClass().getClassLoader().getResource("raster.tif").toURI())
        GeoTIFF geoTIFF = new GeoTIFF()
        Raster raster = geoTIFF.read(file)
        Raster scaled = raster.scale(10, 10)
        assertNotNull scaled
    }

    @Test void zonalStatistics() {
        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data = [
            [0,0,0,0,0,0,0],
            [0,1,1,1,1,1,0],
            [0,1,2,3,2,1,0],
            [0,1,1,1,1,1,0],
            [0,0,0,0,0,0,0]
        ]
        Raster raster = new Raster(data, bounds)
        Layer zones = new Memory().create("zones", [new Field("geom","Geometry","EPSG:4326")])
        bounds.tile(0.5).each{b -> zones.add([b.geometry])}
        Layer stats = raster.zonalStatistics(0, zones)
        assertEquals 4, stats.count
        stats.features.each{f ->
            assertNotNull f.geom
            assertNotNull f.get("count")
            assertNotNull f.get("min")
            assertNotNull f.get("max")
            assertNotNull f.get("sum")
            assertNotNull f.get("avg")
            assertNotNull f.get("stddev")
            assertNull f.get("classification")
        }
    }

    @Test void createFromList() {

        List data = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")

        Raster raster = new Raster(data, bounds)
        assertNotNull raster
        assertNotNull raster.bounds
        assertEquals 0, raster.eval(new Point(0.5,0.5))[0], 0.1
        assertEquals 1, raster.eval(new Point(1.5,1.5))[0], 0.1
        assertEquals 2, raster.eval(new Point(2.5,2.5))[0], 0.1
        assertEquals 3, raster.eval(new Point(3.5,2.5))[0], 0.1
    }

    @Test void invert() {
        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data = [
                [0f,0,0,0,0,0,0],
                [0f,1,1,1,1,1,0],
                [0f,1,2,3,2,1,0],
                [0f,1,1,1,1,1,0],
                [0f,0,0,0,0,0,0]
        ]
        Raster raster = new Raster(data, bounds)
        Raster invertedRaster = raster.invert()
        assertNotNull(invertedRaster)

        /*assertEquals(-0, invertedRaster.eval(new Point(0.5,0.5))[0], 0.1)
        assertEquals(-1, invertedRaster.eval(new Point(1.5,1.5))[0], 0.1)
        assertEquals(-2, invertedRaster.eval(new Point(2.5,2.5))[0], 0.1)
        assertEquals(-3, invertedRaster.eval(new Point(3.5,2.5))[0], 0.1)*/
    }

    @Test void negative() {
        Bounds bounds = new Bounds(0, 0, 7, 5, "EPSG:4326")
        List data = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Raster raster = new Raster(data, bounds)
        Raster invertedRaster = -raster
        assertNotNull(invertedRaster)

        /*assertEquals(-0, invertedRaster.eval(new Point(0.5,0.5))[0], 0.1)
        assertEquals(-1, invertedRaster.eval(new Point(1.5,1.5))[0], 0.1)
        assertEquals(-2, invertedRaster.eval(new Point(2.5,2.5))[0], 0.1)
        assertEquals(-3, invertedRaster.eval(new Point(3.5,2.5))[0], 0.1)*/
    }

    @Test void getPoint() {
        Bounds bounds = new Bounds(7, 7, 17, 15, "EPSG:4326")
        List data = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Raster raster = new Raster(data, bounds)
        Point pt = raster.getPoint(1,2)
        assertEquals 9.14, pt.x, 0.01
        assertEquals 11, pt.y, 0.01
    }

    @Test void getPixel() {
        Bounds bounds = new Bounds(7, 7, 17, 15, "EPSG:4326")
        List data = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Raster raster = new Raster(data, bounds)
        List pixel = raster.getPixel(new Point(9.14, 11))
        assertEquals 1.0, pixel[0], 0.01
        assertEquals 2.0, pixel[1], 0.01
    }

    @Test void contains() {
        Bounds bounds = new Bounds(7, 7, 17, 15, "EPSG:4326")
        List data = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Raster raster = new Raster(data, bounds)
        // Pixel
        assertTrue(raster.contains(0,0))
        assertTrue(raster.contains(1,1))
        assertTrue(raster.contains(6,1))
        assertTrue(raster.contains(6,4))
        assertFalse(raster.contains(-1,3))
        assertFalse(raster.contains(1,-3))
        assertFalse(raster.contains(8,2))
        assertFalse(raster.contains(1,8))
        // Point
        assertTrue(raster.contains(new Point(8,8)))
        assertTrue(raster.contains(new Point(7.1,14)))
        assertTrue(raster.contains(new Point(16,12)))
        assertFalse(raster.contains(new Point(7,14)))
        assertFalse(raster.contains(new Point(6,14)))
        assertFalse(raster.contains(new Point(10,15)))
        assertFalse(raster.contains(new Point(10,21)))
    }

    @Test void getNeighbors() {
        Bounds bounds = new Bounds(7, 7, 17, 15, "EPSG:4326")
        List data = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Raster raster = new Raster(data, bounds)

        Map neighbors = raster.getNeighbors([3,2])
        assertEquals(1, neighbors.nw, 0.1)
        assertEquals(1, neighbors.n, 0.1)
        assertEquals(1, neighbors.ne, 0.1)
        assertEquals(2, neighbors.e, 0.1)
        assertEquals(1, neighbors.se, 0.1)
        assertEquals(1, neighbors.s, 0.1)
        assertEquals(1, neighbors.sw, 0.1)
        assertEquals(2, neighbors.w, 0.1)

        neighbors = raster.getNeighbors([0,0])
        assertNull neighbors.nw
        assertNull neighbors.n
        assertNull neighbors.ne
        assertEquals(0, neighbors.e, 0.1)
        assertEquals(1, neighbors.se, 0.1)
        assertEquals(0, neighbors.s, 0.1)
        assertNull neighbors.sw
        assertNull neighbors.w

        neighbors = raster.getNeighbors([6,4])
        assertEquals(1, neighbors.nw, 0.1)
        assertEquals(0, neighbors.n, 0.1)
        assertNull neighbors.ne
        assertNull neighbors.e
        assertNull neighbors.se
        assertNull neighbors.s
        assertNull neighbors.sw
        assertEquals(0, neighbors.w, 0.1)
    }

    @Test void eachCell() {
        Bounds bounds = new Bounds(7, 7, 17, 15, "EPSG:4326")
        List data = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Raster raster = new Raster(data, bounds)
        raster.eachCell(bounds:[0,0,4,4], band: 0,{v, x, y ->
           println "${v} @ ${x},${y}"
        })
    }

    @Test void eachWindow() {
        Bounds bounds = new Bounds(7, 7, 17, 15, "EPSG:4326")
        List data = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Raster raster = new Raster(data, bounds)
        raster.eachWindow(bounds: [0,0,raster.cols, raster.rows],window: [4,4], key:[0,0], outside: -1, {v, x, y ->
            println "${v} @ ${x},${y}"
        })
    }

    @Test void getValues() {
        Bounds bounds = new Bounds(7, 7, 17, 15, "EPSG:4326")
        List data = [
                [0,0,0,0,0,0,0],
                [0,1,1,1,1,1,0],
                [0,1,2,3,2,1,0],
                [0,1,1,1,1,1,0],
                [0,0,0,0,0,0,0]
        ]
        Raster raster = new Raster(data, bounds)
        List d = raster.getValues(0,0,3,2)
        [0.0, 0.0, 0.0, 0.0, 1.0, 1.0].eachWithIndex{v,i ->
            assertEquals(v, d[i], 0.1)
        }
        d = raster.getValues(0,0,3,2,0,false)
        assertEquals 2, d.size()
        [[0.0, 0.0, 0.0], [0.0, 1.0, 1.0]].eachWithIndex{v1,i1 ->
            assertEquals 3, v1.size()
            v1.eachWithIndex{v2,i2 ->
                assertEquals(v2, d[i1][i2], 0.1)
            }

        }
        d = raster.getValues(1,1,4,3)
        [1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 3.0, 2.0, 1.0, 1.0, 1.0, 1.0].eachWithIndex{v,i ->
            assertEquals(v, d[i], 0.1)
        }
        d = raster.getValues(1,1,4,3,0,false)
        assertEquals 3, d.size()
        [[1.0, 1.0, 1.0, 1.0], [1.0, 2.0, 3.0, 2.0], [1.0, 1.0, 1.0, 1.0]].eachWithIndex{v1,i1 ->
            assertEquals 4, v1.size()
            v1.eachWithIndex{v2,i2 ->
                assertEquals(v2, d[i1][i2], 0.1)
            }

        }
        d = raster.getValues(1,1,4,4,0,false)
        assertEquals 4, d.size()
        [[1.0, 1.0, 1.0, 1.0], [1.0, 2.0, 3.0, 2.0], [1.0, 1.0, 1.0, 1.0],[0.0, 0.0, 0.0, 0.0]].eachWithIndex{v1,i1 ->
            assertEquals 4, v1.size()
            v1.eachWithIndex{v2,i2 ->
                assertEquals(v2, d[i1][i2], 0.1)
            }

        }
    }

    @Test void getData() {
        File file = new File(getClass().getClassLoader().getResource("alki.tif").toURI())
        GeoTIFF geoTIFF = new GeoTIFF()
        Raster raster = geoTIFF.read(file)
        def data = raster.data
        assertNotNull data
        assertTrue data instanceof java.awt.image.Raster
    }

    @Test void getExtrema() {
        File file = new File(getClass().getClassLoader().getResource("alki.tif").toURI())
        GeoTIFF geoTIFF = new GeoTIFF()
        Raster raster = geoTIFF.read(file)
        Map result = raster.extrema
        assertTrue result.containsKey("min")
        assertTrue result.containsKey("max")
        assertEquals 10.0, result.min[0], 0.1
        assertEquals 51.0, result.min[1], 0.1
        assertEquals 58.0, result.min[2], 0.1
        assertEquals 255.0, result.max[0], 0.1
        assertEquals 255.0, result.max[1], 0.1
        assertEquals 250.0, result.max[2], 0.1
    }

    @Test void resample() {
        File file = new File(getClass().getClassLoader().getResource("alki.tif").toURI())
        GeoTIFF geoTIFF = new GeoTIFF()
        Raster raster = geoTIFF.read(file)

        // Size
        Raster raster1 = raster.resample(size: [200,400])
        assertEquals 200.0, raster1.size[0], 1.0
        assertEquals 400.0, raster1.size[1], 1.0

        // BBox
        Bounds bounds = raster.bounds.scale(-2)
        println bounds
        Raster raster2 = raster.resample(bbox: bounds)
        assertEquals bounds.minX, raster2.bounds.minX, 0.1
        assertEquals bounds.minY, raster2.bounds.minY, 0.1
        assertEquals bounds.maxX, raster2.bounds.maxX, 0.1
        assertEquals bounds.maxY, raster2.bounds.maxY, 0.1

        // Rect
        Raster raster3 = raster.resample(rect: [100, 100, 400, 500])
        assertEquals 400.0, raster3.size[0], 1.0
        assertEquals 500.0, raster3.size[1], 1.0
    }

    @Test void getHistogram() {
        File file = new File(getClass().getClassLoader().getResource("alki.tif").toURI())
        GeoTIFF geoTIFF = new GeoTIFF()
        Raster raster = geoTIFF.read(file)
        Histogram histogram = raster.histogram
        assertNotNull histogram
        assertEquals 3, histogram.numberOfBands
        (0..<histogram.numberOfBands).each{b ->
            // println "Band ${b}"
            // println "   Counts: ${histogram.counts(b)}"
            // println "   Bins  : ${histogram.bins(b)}"
            assertEquals 256, histogram.counts(b).size()
            assertEquals 256, histogram.bins(b).size()
        }
        // count
        assertEquals 14, histogram.count(15,0)
        assertEquals 2440, histogram.count(145,1)
        assertEquals 4002, histogram.count(201,2)

        // count via getAt
        assertEquals 14, histogram[15]
        assertEquals 2440, histogram[[145,1]]
        assertEquals 4002, histogram[[201,2]]

        // bin
        List values = histogram.bin(144,0)
        assertEquals 144.0, values[0], 0.1
        assertEquals 145.0, values[1], 0.1

        values = histogram.bin(56,1)
        assertEquals 56.0, values[0], 0.1
        assertEquals 57.0, values[1], 0.1

        values = histogram.bin(245,2)
        assertEquals 245.0, values[0], 0.1
        assertEquals 246.0, values[1], 0.1
    }

    @Test void stylize() {
        File file = new File(getClass().getClassLoader().getResource("raster.tif").toURI())
        GeoTIFF geoTIFF = new GeoTIFF()
        Raster raster = geoTIFF.read(file)
        raster.style = new ColorMap([[color: "#008000", quantity: 70], [color: "#663333", quantity: 256]])
        Raster raster2 = raster.stylize()
        assertTrue raster.eval(10,10) != raster2.eval(10,10)
        Symbolizer sym = new ColorMap([
                [color: "#000000", quantity: 70],
                [color: "#0000FF", quantity: 110],
                [color: "#00FF00", quantity: 135],
                [color: "#FF0000", quantity: 160],
                [color: "#FF00FF", quantity: 185],
                [color: "#FFFF00", quantity: 210],
                [color: "#00FFFF", quantity: 235],
                [color: "#FFFFFF", quantity: 256]
        ])
        Raster raster3 = raster.stylize(sym)
        assertTrue raster.eval(10,10) != raster3.eval(10,10)
    }
}