include "alldifferent.mzn";
include "decreasing.mzn";
include "increasing.mzn";
include "arg_sort.mzn";
include "sort.mzn";
include "nvalue.mzn";
include "gecode.mzn";


string: Name; % name of datafile 
int: n = length(Demand); % n - number of customers
int: NumVehicles; % - number of vehicles available
int: Cap;
array [1..NumVehicles] of int: Capacities = [Cap | i in 1..NumVehicles]; % Capacities of vehicles
array [int] of int: Demand; % each customers demand
array [1..n+1] of float: locX; % each customer location (x coordinate)
array [1..n+1] of float: locY; % each customer location (y coordinate)
float: DepotX = locX[n+1]; % x coordinate of depot
float: DepotY = locY[n+1]; % y coordinate of depot


/*
calculate distances - we don't take Earth's roundiness into account. 
For more accurate distances see: https://en.wikipedia.org/wiki/Haversine_formula
All distances are rounded. All distances are symmetrical
*/
function var int: sqrt_and_round(var float: x) :: promise_total =
         let { var int: y;
               constraint y >= 0;
               constraint y * y < x /\ x < (y+1)*(y+1); } in y + 1;

function var int: calculate_distance(var float: x1,var float: y1,var float: x2,var float: y2) = let {
  var float: diff_x = coords_dist(x1, x2);
  var float: diff_y = coords_dist(y1, y2);
  } in sqrt_and_round(diff_x*diff_x + diff_y*diff_y);
function var float: coords_dist(var float: x, var float: y) = if x > 0 /\ y > 0 then abs(x-y) else
  if x < 0 /\ y < 0 then abs(abs(x) - abs(y)) else
  abs(x) + abs(y) endif endif;  


% which of vehicles will handle customer?
% vc[i] = j <-> i-th customer will be handled by j-th vehicle
% note, that we may leave a vehicle unused
array [1..n] of var 1..NumVehicles: vehicle_to_customer;

constraint forall(v in 1..NumVehicles)
  (Capacities[v] >= sum([
    if vehicle_to_customer[i] == v then
      Demand[i]
    else
      0
    endif
    | i in 1..n
  ]));


array [1..n] of var 1..n: order :: domain;
constraint alldifferent(order);


var int: total_distance :: promise_total = 
  sum([
    if i == 1 then
      calculate_distance(locX[order[i]], locY[order[i]], DepotX, DepotY)
    else if i == n then
    calculate_distance(locX[order[n]], locY[order[n]], DepotX, DepotY)
    else if vehicle_to_customer[order[i]] == vehicle_to_customer[order[i+1]] then
      calculate_distance(locX[order[i]], locY[order[i]], locX[order[i+1]], locY[order[i+1]])
    else
      calculate_distance(locX[order[i]], locY[order[i]], DepotX, DepotY) + calculate_distance(locX[order[i+1]], locY[order[i+1]], DepotX, DepotY)
    endif endif endif
    | i in 1..n
  ]);
    

var int: used_vehicles;
constraint nvalue(used_vehicles, vehicle_to_customer);


var int: objective_function = 10000 * used_vehicles + total_distance;


ann: search_vc_ann = int_search(vehicle_to_customer, input_order, indomain_max);
ann: search_order_ann = int_search(order, dom_w_deg, indomain_random);
ann: search_ann = seq_search([search_vc_ann, search_order_ann]);
ann: restart_ann;
ann: lns;
% restart_ann = restart_luby(10*n);
% lns = relax_and_reconstruct(order, 50);

solve ::search_ann ::restart_ann ::lns minimize objective_function;

output ["n = \(n)\n"]
         ++ ["Capacities = \(Capacities)\n"]
         ++ ["Demand = \(Demand)\n"]         
         ++ ["vehicle_to_customer = \(vehicle_to_customer)\n"]
         ++ ["order = \(order)\n"]
         ++ ["total_distance = \(total_distance)\n"]
         ++ ["objective_function = \(objective_function)\n"]
         ++ ["used_vehicles = \(used_vehicles)\n"]
         ;
