package com.android.mathias.velocity;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

class RecyclerAdapterRoutes extends RecyclerView.Adapter<RecyclerAdapterRoutes.RouteCardHolder> {
    private List<Route> mRouteList;

    class RouteCardHolder extends RecyclerView.ViewHolder {
        TextView mRouteName;
        TextView mRouteStartPoint;
        TextView mRouteEndPoint;
        TextView mRouteDistance;

        RouteCardHolder(View view) {
            super(view);
            mRouteName = (TextView) view.findViewById(R.id.txt_route_name);
            mRouteStartPoint = (TextView) view.findViewById(R.id.txt_route_start_point);
            mRouteEndPoint = (TextView) view.findViewById(R.id.txt_route_end_point);
            mRouteDistance = (TextView) view.findViewById(R.id.txt_route_distance);
        }
    }

    RecyclerAdapterRoutes(List<Route> routes) {
        mRouteList = routes;
    }

    @Override
    public RouteCardHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_route, parent, false);
        return new RouteCardHolder(view);
    }

    @Override
    public void onBindViewHolder(RouteCardHolder holder, int position) {
        Route route = mRouteList.get(position);
        holder.mRouteName.setText(route.getName());
        holder.mRouteStartPoint.setText(String.format("From: %s", route.getStartName()));
        holder.mRouteEndPoint.setText(String.format("To: %s", route.getEndName()));
        holder.mRouteDistance.setText(String.format("Distance: %.1fm", route.getApproximateDistance()));
    }

    @Override
    public int getItemCount() {
        return mRouteList.size();
    }
}