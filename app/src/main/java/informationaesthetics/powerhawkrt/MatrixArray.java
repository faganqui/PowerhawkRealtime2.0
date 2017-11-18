package informationaesthetics.powerhawkrt;

import java.util.ArrayList;

/**
 * Created by quinn on 25/06/17.
 */

/* matrix form:
    - Saved as array of strings
    - each string is in the form:
        "row1item1,row1item2,row1item3,;row2item1,row2item2,row2item3,;row3item1,row3item2,row3item3,;"
    - could be any row/column length (however each row will always contain same number of items)
*/


public class MatrixArray {
    private ArrayList<String> theMatrix = new ArrayList<>();
    private ArrayList<ArrayList> time = new ArrayList<>(); //contains table values over time
    private ArrayList<ArrayList> rows;  // contains arrays of row elements
    private ArrayList<Double> row;    // contains elements of each row

    public MatrixArray (String inputMatrix) {
        add_matrix(inputMatrix);
    }

    public Double get_recent_item(int row_pos, int col_pos){
        //returns the most recently saved item from a certain matrix position
        ArrayList<Double> temp_return_row = (ArrayList<Double>) time.get(time.size()-1).get(row_pos);
        return temp_return_row.get(col_pos);
    }

    public ArrayList<Double> get_all_items(int row_pos, int col_pos){
        //returns all saved entries of a certain point in matrix from oldest to newest
        ArrayList<Double> returnList = new ArrayList<>();
        for(ArrayList<ArrayList> temp_rows : time) {
            Double item = (Double) temp_rows.get(row_pos).get(col_pos);
            returnList.add(item);
        }
        return returnList;

    }

    public void add_matrix(String inputMatrix){
        //adds the matrix string to the arrays

        rows = new ArrayList<>();
        for(String cur_row : inputMatrix.split(";")){
            if(cur_row.split(",").length > 2) {
                row = new ArrayList<>();
                for (String element : cur_row.split(",")) {
                    if(element != ""){
                        row.add(Double.valueOf(element));
                    }
                }
                rows.add(row);
            }
        }
        if(time.size() > 100) { //making sure we dont store too much data
            time.remove(0);
            time.add(rows);
        }else{
            time.add(rows);
        }
    }

    public int getHeight(){ //returns the number of rows
        return time.get(0).size();
    }

    public int getWidth(){ //returns the number of columns
        return ((ArrayList<Double>) time.get(0).get(0)).size();
    }

}
