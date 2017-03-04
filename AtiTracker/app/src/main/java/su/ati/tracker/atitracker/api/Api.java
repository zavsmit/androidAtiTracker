package su.ati.tracker.atitracker.api;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import su.ati.tracker.atitracker.api.model.CheckStatus;
import su.ati.tracker.atitracker.api.model.Point;
import su.ati.tracker.atitracker.api.model.SendPhoto;
import su.ati.tracker.atitracker.api.model.SetPoint;
import su.ati.tracker.atitracker.api.model.StartRide;

/**
 * Created by Zavsmit on 04.03.2017.
 */

public class Api {

    public static final String API_URL = "http://ec43419d.ngrok.io";

    public interface Location {
        @POST("/startRide/{rideId}/")
        Call<ResponseBody> startRide(@Path("rideId") String rideId, @Body StartRide user);

        @POST("/setPoint/{rideId}/")
        Call<SetPoint> setPoint(@Path("rideId") String rideId, @Body List<Point> points);

        @POST("/endRide/{rideId}/")
        Call<SetPoint> endRide(@Path("rideId") String rideId, @Body Point point);
    }


    public interface Checked {
        @GET("/checkStatus/{rideId}/")
        Call<CheckStatus> checkStatus(@Path("rideId") String rideId);

        @POST("/sendPhoto/{rideId}/")
        Call<ResponseBody> sendPhoto(@Path("rideId") String rideId,  @Body SendPhoto point);
    }
}
