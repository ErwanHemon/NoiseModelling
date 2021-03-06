/**
 * @Author Nicolas Fortin, Université Gustave Eiffel
 * @Author Pierre Aumond, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Import_and_Export

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.io.FileType
import org.apache.commons.io.FilenameUtils
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.functions.io.csv.CSVDriverFunction
import org.h2gis.functions.io.dbf.DBFDriverFunction
import org.h2gis.functions.io.geojson.GeoJsonDriverFunction
import org.h2gis.functions.io.gpx.GPXDriverFunction
import org.h2gis.functions.io.osm.OSMDriverFunction
import org.h2gis.functions.io.shp.SHPDriverFunction
import org.h2gis.functions.io.tsv.TSVDriverFunction
import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.TableLocation

import java.sql.Connection
import java.sql.Statement

title = 'Import Files from folder'
description = 'Import all files from a folder (csv, dbf, geojson, gpx, bz2, gz, osm, shp, tsv) to tables'

inputs = [
        pathFile: [name: 'Path of the folder', description : 'Path of the folder', title: 'Path of the folder', type: String.class],
        databaseName: [name: 'Name of the database', title: 'Name of the database', description : 'Name of the database (default : first found db)', min : 0, max : 1, type: String.class],
        defaultSRID   : [name: 'Default SRID', title: 'Default SRID', description: 'If the layer does not include SRID properties, it will take this value (default : 4326)', min: 0, max: 1, type: Integer.class],
        user_ext: [name: 'Extension to import', description : 'Extension to import (shp, csv, etc.). Don\'t use the dot !', title: 'Extension to import', type: String.class]
]

outputs = [
        tableNameCreated: [name: 'tableNameCreated', title: 'tableNameCreated', type: String.class]
]


static Connection openGeoserverDataStoreConnection(String dbName) {
        if(dbName == null || dbName.isEmpty()) {
                dbName = new GeoServer().catalog.getStoreNames().get(0)
        }
        Store store = new GeoServer().catalog.getStore(dbName)
        JDBCDataStore jdbcDataStore = (JDBCDataStore)store.getDataStoreInfo().getDataStore(null)
        return jdbcDataStore.getDataSource().getConnection()
}

def run(input) {

        // Get name of the database
        String dbName = ""
        if (input['databaseName']){dbName = input['databaseName'] as String}

        Integer defaultSRID = 4326
        if (input['defaultSRID']) {
                defaultSRID = input['defaultSRID'] as Integer
        }

        // Open connection
        openGeoserverDataStoreConnection(dbName).withCloseable { Connection connection ->

                String user_ext = input["user_ext"] as String
                String folder = input["pathFile"] as String
                String outputTableName_full = ""

                def dir = new File(folder)
                dir.eachFileRecurse (FileType.FILES) { file ->

                        String pathFile = file as String
                        String ext = pathFile.substring(pathFile.lastIndexOf('.') + 1, pathFile.length())

                        if (ext ==  user_ext){
                                String fileName = FilenameUtils.removeExtension(new File(pathFile).getName())
                                String outputTableName = fileName
                                outputTableName = outputTableName.toUpperCase()

                                Statement stmt = connection.createStatement()
                                String dropOutputTable = "drop table if exists " + outputTableName
                                stmt.execute(dropOutputTable)

                                switch(ext) {
                                        case "csv":
                                                CSVDriverFunction csvDriver = new CSVDriverFunction()
                                                csvDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                                                outputTableName_full = outputTableName + " & " + outputTableName_full
                                                break
                                        case "dbf":
                                                DBFDriverFunction dbfDriver = new DBFDriverFunction()
                                                dbfDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                                                outputTableName_full = outputTableName + " & " + outputTableName_full
                                                break
                                        case "geojson":
                                                GeoJsonDriverFunction geoJsonDriver = new GeoJsonDriverFunction()
                                                geoJsonDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                                                outputTableName_full = outputTableName + " & " + outputTableName_full
                                                break
                                        case "gpx":
                                                GPXDriverFunction gpxDriver = new GPXDriverFunction()
                                                gpxDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                                                outputTableName_full = outputTableName + " & " + outputTableName_full
                                                break
                                        case "bz2":
                                                OSMDriverFunction osmDriver = new OSMDriverFunction()
                                                osmDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                                                outputTableName_full = outputTableName + " & " + outputTableName_full
                                                break
                                        case "gz":
                                                OSMDriverFunction osmDriver = new OSMDriverFunction()
                                                osmDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                                                outputTableName_full = outputTableName + " & " + outputTableName_full
                                                break
                                        case "osm":
                                                OSMDriverFunction osmDriver = new OSMDriverFunction()
                                                osmDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                                                outputTableName_full = outputTableName + " & " + outputTableName_full
                                                break
                                        case "shp":
                                                SHPDriverFunction shpDriver = new SHPDriverFunction()
                                                shpDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                                                outputTableName_full = outputTableName + " & " + outputTableName_full
                                                break
                                        case "tsv":
                                                TSVDriverFunction tsvDriver = new TSVDriverFunction()
                                                tsvDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                                                outputTableName_full = outputTableName + " & " + outputTableName_full
                                                break
                                }
                                int srid = SFSUtilities.getSRID(connection, TableLocation.parse(outputTableName))
                                if(srid == 0) {
                                        connection.createStatement().execute(String.format("UPDATE %s SET THE_GEOM = ST_SetSRID(the_geom,%d)",
                                                TableLocation.parse(outputTableName).toString(), defaultSRID))

                                }

                        }
                }

                return [tableNameCreated : "The table(s) " + outputTableName_full + " has/have been uploaded to database !"]
        }
}
