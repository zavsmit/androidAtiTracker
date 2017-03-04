package su.ati.tracker.atitracker.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by Zavsmit on 04.03.2017.
 */

public class Api {

    public static final String API_URL = "http://ec43419d.ngrok.io";

    public static class Contributor {
        public final String login;
        public final int contributions;

        public Contributor(String login, int contributions) {
            this.login = login;
            this.contributions = contributions;
        }
    }


    public interface AtiTracker {
        @GET("/repos/{owner}/{repo}/contributors")
        Call<List<Contributor>> contributors();
    }
}
