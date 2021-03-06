package es.tid.haewoon.geotaggedtweets;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import processing.core.PApplet;
import toxi.geom.Polygon2D;
import toxi.geom.Vec2D;
import toxi.geom.mesh2d.Voronoi;
import toxi.processing.ToxiclibsSupport;
import twitter4j.GeoLocation;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.json.DataObjectFactory;
import codeanticode.glgraphics.GLConstants;
import de.fhpotsdam.unfolding.Map;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.utils.MapUtils;

/*
 * With Unfolding 0.8.0
 */
public class WithGoogleMap extends PApplet {
    /**
     * automatically generated serialVersionUID
     */
    private static final long serialVersionUID = 1L;
    Map map;
    private static final int MAX_TWEETS = 3000;
    private static final int INTERVAL = 100; // msec
    private ArrayList<Status> statuses;
    private ArrayList<TransparentMarker> markers;

    private static final float WIDTH = 1000f;
    private static final float HEIGHT = 1000f;
    private static final int MAX_NUMBER_OF_MARKERS = 150;
    private static final int MARKER_SIZE = 20;

    ToxiclibsSupport gfx;
    Voronoi voronoi;
    
    private int iteratorIndex;
    private int savedTime = 0;
    private int opacity = 0;
    public void setup() {
        size((int)WIDTH, (int)HEIGHT, GLConstants.GLGRAPHICS);
        smooth();
        createFont("Arial", 10);
        
        gfx = new ToxiclibsSupport( this );

        map = new Map(this, new Google.GoogleMapProvider());
        //         map = new Map(this, new Microsoft.RoadProvider());
        //         map = new Map(this, new Yahoo.HybridProvider());
        map.zoomAndPanTo(new Location(53.3f, -4.41f), 6);

        MapUtils.createDefaultEventDispatcher(this, map);

        iteratorIndex = 0;

        try {
            readJson("/workspace/twitter/UK1");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void draw() {
        map.draw();
        voronoi = new Voronoi();

        for (int i = 0; i < markers.size(); i++) {
            int passedTime = millis() - savedTime;

            opacity = (int) map(min(iteratorIndex - i, MAX_NUMBER_OF_MARKERS), 0, MAX_NUMBER_OF_MARKERS , 150, 0);
            
            TransparentMarker tm = markers.get(i);
            
            drawTweet(tm, opacity);

            if (passedTime > INTERVAL) {
                iteratorIndex = (iteratorIndex+1) % MAX_TWEETS;
                savedTime = millis();
            }

            if (i > iteratorIndex % MAX_TWEETS) {
                // sophisticated process needed
                break;
            }
            
            float xy[] = this.map.getScreenPositionFromLocation(tm.location);
            float screenX = xy[0];
            float screenY = xy[1];
         
            if (tm.isVisible()) {
                voronoi.addPoint(new Vec2D(screenX, screenY));
            }
        }
        
        noFill();
        stroke(0);
        
        for ( Polygon2D polygon : voronoi.getRegions() ) {
            gfx.polygon2D( polygon );
        }
        
    }

    public void drawTweet(TransparentMarker tm, int opacity) {
        fill(222, 81, 36, opacity);
        float xy[] = this.map.getScreenPositionFromLocation(tm.location);
        float screenX = xy[0];
        float screenY = xy[1];
        
        if (opacity == 0) {
            tm.setVisible(false);
        } else {
            tm.setVisible(true);
        }
        
        ellipse(screenX, screenY, 20, 20);
        noStroke();
        //        ellipse(x, y, 30, 30);
        //        fill(200, 200, 0, 100);
        //        ellipse(x, y, 20, 20);
        //        fill(255, 200);
        //        ellipse(x, y, 10, 10);
        if (tm.hovered) {
            fill(0, 200);
            noStroke();
            rect(tm.screenX + 1, tm.screenY - 15, textWidth(tm.getText()) + 2, 12);
            fill(-256, 200);
            text(tm.getText(), tm.screenX + 2, tm.screenY - 5);
        }
    }

    public void mouseMoved() {
        for (int i = 0; i < iteratorIndex; i++) {
            TransparentMarker tm = markers.get(i);
            if (tm.isHovered(mouseX,  mouseY, MARKER_SIZE)) {
                tm.setHovered(true);
                break;
            }
            tm.setHovered(false);
        }
    }


    private void readJson(String filename) throws IOException {
        statuses = new ArrayList<Status>();
        markers = new ArrayList<TransparentMarker>();

        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        int count = 0;
        try {
            while((line = br.readLine()) != null) {
                // do something with line.
                if (count >= MAX_TWEETS) {
                    break;
                }

                Status s = readStatus(line);
                statuses.add(s);
                GeoLocation gl = s.getGeoLocation();
                if (gl != null) {
                    markers.add(new TransparentMarker(s, gl));
                    count++;
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TwitterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            br.close();
        }
    }


    private Status readStatus(String s) throws TwitterException {
        return DataObjectFactory.createStatus(s);
    }
}
