package su.ati.tracker.atitracker.api.model;

/**
 * Created by Zavsmit on 04.03.2017.
 */

public class StartRide  extends Point{
    private long money;
    private Point startPoint;
    private Point endPoint;

    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public Point getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(Point startPoint) {
        this.startPoint = startPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Point endPoint) {
        this.endPoint = endPoint;
    }
}
