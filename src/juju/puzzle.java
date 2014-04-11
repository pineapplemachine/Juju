/*
 * Juju is Sudoku written in Java! This program and its source code are
 * released as public domain. (But that doesn't mean you should claim credit
 * that isn't yours, or sell it when it could otherwise be had for free, 
 * because that would be a shitty thing of you to do.)
 * Juju was written by Sophie Kirschner. Visit the author's website at
 * http://pineapplemachine.com
 */

package juju;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

// this class contains data for a sudoku puzzle
public class puzzle {
    // debug stuff, for debugging the code
    boolean debug=false;
    private void debuglog(String message){if(debug){System.out.println(message);}}
    // default puzzle is used when random puzzle generation hiccups
    final private static int[] defaultpuzzledata={3,1,7,9,4,6,5,8,2,9,4,5,2,8,3,6,7,1,8,6,2,5,7,1,3,9,4,1,9,4,7,6,5,8,2,3,7,2,3,4,1,8,9,5,6,5,8,6,3,2,9,1,4,7,6,7,1,8,9,2,4,3,5,4,3,9,1,5,7,2,6,8,2,5,8,6,3,4,7,1,9};
    // puzzle data
    int sqsize,sqsize2; // how big are the squares? (typical sudoku is 3x3)
    int[] data; // complete puzzle data
    boolean[] hidden; // which values start as unknowns in the puzzle?
    // constructors
    public puzzle(int squaresize){
        // work out sqsize variables
        sqsize=squaresize;
        sqsize2=sqsize*sqsize;
        int sqsize4=sqsize2*sqsize2;
        // initialize arrays
        data=new int[sqsize4];
        hidden=new boolean[sqsize4];
    }
    public puzzle(int squaresize,int maxremoved,int maxcycles){
        // work out sqsize variables
        sqsize=squaresize;
        sqsize2=sqsize*sqsize;
        int sqsize4=sqsize2*sqsize2;
        // initialize arrays
        data=new int[sqsize4];
        hidden=new boolean[sqsize4];
        // generate the puzzle
        generate(new Random(),maxremoved,maxcycles);
    }
    public puzzle(int[] puzdata){
        boolean ok=setpuzzledata(puzdata);
        if(!ok){
            sqsize=main.squaresize;
            sqsize2=sqsize*sqsize;
            int sqsize4=sqsize2*sqsize2;
            // initialize arrays
            data=new int[sqsize4];
            hidden=new boolean[sqsize4];
            setpuzzledata(defaultpuzzledata);
        }
    }
    final public boolean setpuzzledata(int[] puzdata){
        debuglog("setting puzzle data using an array of length "+puzdata.length);
        double nsqsize2=Math.pow(puzdata.length,.5);
        double nsqsize=Math.pow(nsqsize2,.5);
        if(!(nsqsize==(int)nsqsize && nsqsize2==(int)nsqsize2)){return false;}
        sqsize=(int)nsqsize; sqsize2=(int)nsqsize2;
        data=puzdata.clone();
        hidden=new boolean[puzdata.length];
        boolean anyhidden=false;
        for(int i=0;i<puzdata.length;i++){
            hidden[i]=(data[i]==0);
            anyhidden|=hidden[i];
        }
        if(!anyhidden){generate(new Random(),main.maxremoved,main.maxcycles);}
        return true;
    }
    // make a string representation of the puzzle, useful for debugging.
    @Override
    public String toString(){
        String str="";
        for(int y=0;y<sqsize2;y++){
            for(int x=0;x<sqsize2;x++){
                if(gethidden(x,y)){
                    str+=" ";
                }else{
                    str+=Integer.toString(getdata(x,y));
                }
                if(x<sqsize2-1){str+=" ";}
            }
            if(y<sqsize2-1){str+="\n";}
        }
        return str;
    }
    // make a string for saving as a cookie
    public String tocookie(){
        String str="";
        for(int i=0;i<data.length;i++){
            str+=Integer.toString(data[i]);
            str+=hidden[i]?"1":"0";
        }
        return str;
    }
    // read puzzle data from a cookie string
    public void fromcookie(String str,int start){
        if(str.length()-start>=(data.length<<1)){
            for(int i=0;i<data.length;i++){
                data[i]=str.charAt(start+(i<<1));
                hidden[i]=(str.charAt(start+(i<<1)+1)=='1');
            }
        }
    }
    // generate the puzzle. returns the number of positions hidden by hide().
    final public int generate(Random rand,int maxremoved,int maxcycles){
        debuglog("generating a puzzle");
        boolean ok=fill(rand,30000);
        if(!ok){debuglog("took too many tries; using default puzzle instead.");data=defaultpuzzledata;}
        int removed=hide(rand,maxremoved,maxcycles);
        return removed;
    }
    // check if it's possible to solve the puzzle (without trial-and-error guesswork, anyway)
    public LinkedList<int[]> hints;
    final public boolean solve(){return solve(false,hidden.clone(),data);}
    final public boolean solve(boolean onlyonce,boolean[] solvehidden,int[] puzdata){
        // for keeping track of how many corrections have been made, and how many would need to be made to solve the whole puzzle
        int fixes=0,totalfixes=0;
        for(int i=0;i<solvehidden.length;i++){totalfixes+=solvehidden[i]?1:0;}
        // was an incorrect number discovered, or was a spot encountered where nothing fits?
        boolean error=false;
        // legality map, records which numbers are allowed to go where
        boolean[][] legalmap=getlegalmap(solvehidden,puzdata);
        // are we using this function as an attempt to gather hints?
        LinkedList<int[]> fixlist=new LinkedList<>();
        // loops until all fixes have been made, no more fixes can be made, or until an invalid deduction occurs
        do{
            fixlist.clear();
            // check for "naked pairs", "naked triples", and so on
            // TODO: this is hard
            // check for "hidden pairs", "hidden triples", and so on
            //solvehiddentuples(legalmap,puzdata); // doesn't work
            // look for places where only one number is legal
            error=error || solveonelegalforcell(fixlist,legalmap,puzdata);
            // look for rows/columns/squares which only have one legal placement for a number
            error=error || solveonelegalforgroup(fixlist,legalmap,puzdata);
            // apply the found fixes
            for(int[] fix:fixlist){
                solvereveal(solvehidden,legalmap,puzdata,fix);
                fixes++;
                hints=fixlist;
            }
        }while(!(fixlist.isEmpty() || error || onlyonce));
        if(error && !onlyonce){debuglog("failed to test if puzzle was solveable; encountered an error");}
        return (fixes==totalfixes && !error);
    }
    // look for sets of n numbers shared between cells in a group and mark other numbers as illegal
    // (for example, suppose you had two cells in a row with 1 and 2 as legal values, and these values were legal nowhere else in the group. all values except for 1 and 2 are therefore illegal in those cells.)
    private void solvehiddentuples(boolean[][] legalmap,int[] puzdata){
        for(int k=0;k<3;k++){
            for(int i=0;i<sqsize2;i++){
                // get the group
                boolean group[][]=new boolean[sqsize2][];
                for(int j=0;j<sqsize2;j++){
                    int coord[]=getsolvecoord(i,j,k);
                    group[j]=getlegal(legalmap,coord[0],coord[1]);
                }
                // shuffle the info around a bit so it's easier to work with
                boolean[][] mask=new boolean[sqsize2+1][];
                for(int a=1;a<=sqsize2;a++){
                    mask[a]=new boolean[sqsize2];
                    for(int b=0;b<sqsize2;b++){
                        if(group[b]!=null){mask[a][b]=group[b][a];}
                    }
                }
                // find groups of hidden candidates, if any exist
                int[] hiddenthings=null;
                while(true){
                    hiddenthings=explorehidden(mask);
                    // handle the thing
                    if(hiddenthings!=null){
                        debuglog("well how about that, found a hidden "+hiddenthings.length+"-tuple");
                        for(int j=0;j<sqsize2;j++){
                            int coord[]=getsolvecoord(i,j,k);
                            boolean[] legal=getlegal(legalmap,coord[0],coord[1]);
                            if(legal!=null){
                                for(int a=0;a<hiddenthings.length;a++){
                                    if(legal[a]){
                                        for(int b=1;b<=sqsize2;b++){
                                            if(legal[b]){
                                                boolean ishidden=false;
                                                for(int c=0;c<hiddenthings.length;c++){if(b==hiddenthings[c]){ishidden=true;break;}}
                                                legal[b]=ishidden;
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }else{
                        break;
                    }
                }
            }
        }
    }
    private int[] explorehidden(boolean[][] mask){
        for(int a=1;a<=sqsize2;a++){
            int[] result=explorehidden(mask,mask[a],mask[a],a+1,0);
            if(result!=null){
                result[0]=a; return result;
            }
        }
        return null;
    }
    private int[] explorehidden(boolean[][] mask,boolean[] thisandmask,boolean[] thisormask,int start,int recursion){
        for(int a=start;a<=sqsize2;a++){
            thisandmask=arrayAND(mask[a],thisandmask);
            thisormask=arrayOR(mask[a],thisormask);
            int numtrue=arrayevaluate(thisormask);
            if(numtrue==start+1){
                // it's a hidden tuple! make sure it's useful to the solver.
                int[] result=new int[recursion+2];
                result[recursion+1]=a; return result;
            }else if(start<=3){
                int[] result=explorehidden(mask,thisandmask,thisormask,a+1,recursion+1);
                if(result!=null){
                    result[recursion+1]=a; return result;
                }
            }
        }
        return null;
    }
    // treat boolean arrays like bitmasks
    private int arrayevaluate(boolean[] group){
        int sum=0;
        for(int i=0;i<group.length;i++){
            sum+=group[i]?1:0;
        }
        return sum;
    }
    private boolean[] arrayAND(boolean[] group1,boolean[] group2){
        boolean[] result=group1.length>group2.length?group1.clone():group2.clone();
        for(int i=0;i<Math.min(group1.length,group2.length);i++){
            result[i]=group1[i] & group2[i];
        }
        return result;
    }
    private boolean[] arrayOR(boolean[] group1,boolean[] group2){
        boolean[] result=group1.length>group2.length?group1.clone():group2.clone();
        for(int i=0;i<Math.min(group1.length,group2.length);i++){
            result[i]=group1[i] & group2[i];
        }
        return result;
    }
    // pretty straightforward: iterate through the legal map looking for cells with only one legal answer
    private boolean solveonelegalforcell(LinkedList<int[]> fixlist,boolean[][] legalmap,int[] puzdata){
        boolean error=false;
        for(int x=0;x<sqsize2;x++){
            for(int y=0;y<sqsize2;y++){
                boolean[] legal=getlegal(legalmap,x,y);
                if(legal!=null){
                    int numlegal=0, lastlegal=0;
                    for(int z=1;z<=sqsize2;z++){
                        if(legal[z]){numlegal++; lastlegal=z;}
                    }
                    if(numlegal==1){
                        int correctnum=getdata(puzdata,x,y);
                        if(lastlegal==correctnum || correctnum==0){
                            addfix(fixlist,x,y,lastlegal);
                        }else{
                            debuglog("only one number legal for cell, but it's the wrong one");
                            error=true; break;
                        }
                    }else if(numlegal==0){
                        debuglog("no legal numbers for cell");
                        error=true; break;
                    }
                }
            }
        }
        return error;
    }
    // iterate through groups of cells (rows/columns/squares) and look for ones where there is only one legal placement for some number.
    private boolean solveonelegalforgroup(LinkedList<int[]> fixlist,boolean[][] legalmap,int[] puzdata){
        boolean error=false;
        for(int i=0;i<sqsize2;i++){
            for(int k=0;k<3;k++){
                int legalplaces[]=new int[sqsize2+1];
                int legalat[]=new int[sqsize2+1];
                boolean revealed[]=new boolean[sqsize2+1];
                for(int j=0;j<sqsize2;j++){
                    int coord[]=getsolvecoord(i,j,k);
                    boolean legal[]=getlegal(legalmap,coord[0],coord[1]);
                    if(legal!=null){
                        for(int l=1;l<=sqsize2;l++){
                            if(legal[l]){legalplaces[l]++; legalat[l]=j;}
                        }
                    }else{
                        revealed[getdata(puzdata,coord[0],coord[1])]=true;
                    }
                }
                for(int l=1;l<=sqsize2;l++){
                    if(!revealed[l]){
                        String[] errorstr={"row","column","square"};
                        int[] coord=getsolvecoord(i,legalat[l],k);
                        if(legalplaces[l]==1){
                            int correctnum=getdata(puzdata,coord[0],coord[1]);
                            if(l==correctnum || correctnum==0){
                                addfix(fixlist,coord,l);
                            }else{
                                debuglog("only one number legal for "+errorstr[k]+", but it's the wrong one");
                                error=true; break;
                            }
                        }else if(legalplaces[l]==0){
                            debuglog("no legal numbers for "+errorstr[k]);
                            error=true; break;
                        }
                    }
                }
            }
        }
        return error;
    }
    private boolean addfix(LinkedList<int[]> fixlist,int[] coord,int solution){
        for(int[] check:fixlist){
            if(check[0]==coord[0] && check[1]==coord[1]){
                if(check[2]!=solution){debuglog("decided two different solutions for the same cell");}
                return false;
            }
        }
        int[] fix={coord[0],coord[1],solution};
        fixlist.addLast(fix);
        return true;
    }
    private boolean addfix(LinkedList<int[]> fixlist,int x,int y,int solution){
        int[] coord={x,y};
        return addfix(fixlist,coord,solution);
    }
    // iterate through each position in the puzzle, build up a map of which numbers are legal where
    private boolean[][] getlegalmap(boolean[] solvehidden,int[] puzdata){
        // initialize the array
        boolean[][] legalmap=new boolean[sqsize2*sqsize2][];
        for(int i=0;i<legalmap.length;i++){
            if(solvehidden[i]){
                legalmap[i]=new boolean[sqsize2+1];
                Arrays.fill(legalmap[i],true);
            }else{
                legalmap[i]=null;
            }
        }
        // figure out what belongs in the array
        for(int i=0;i<sqsize2;i++){
            for(int j=0;j<sqsize2;j++){
                // do this for rows, columns, and squares
                for(int k=0;k<3;k++){
                    int[] coord=getsolvecoord(i,j,k);
                    if(!gethidden(solvehidden,coord[0],coord[1])){
                        int num=getdata(puzdata,coord[0],coord[1]);
                        for(int l=0;l<sqsize2;l++){
                            int[] ncoord=getsolvecoord(i,l,k);
                            boolean[] legal=getlegal(legalmap,ncoord[0],ncoord[1]);
                            if(legal!=null){legal[num]=false;}
                        }
                    }
                }
            }
        }
        return legalmap;
    }
    // reveal a coord in the puzzle and adjust the legality map accordingly
    private void solvereveal(boolean[] solvehidden,boolean[][] legalmap,int[] puzdata,int[] fix){
        if(gethidden(solvehidden,fix[0],fix[1])){
            int num=getdata(puzdata,fix[0],fix[1]);
            if(num==0){
                setdata(puzdata,fix[0],fix[1],fix[2]);
                num=fix[2];
            }
            setlegal(legalmap,fix[0],fix[1],null);
            sethidden(solvehidden,fix[0],fix[1],false);
            int row=fix[1], col=fix[0], sq=(fix[0]/sqsize)+(fix[1]/sqsize)*sqsize;
            int[] solvecoords={row,col,sq};
            for(int j=0;j<sqsize2;j++){
                for(int k=0;k<3;k++){
                    int[] coord=getsolvecoord(solvecoords[k],j,k);
                    boolean[] legal=getlegal(legalmap,coord[0],coord[1]);
                    if(legal!=null){legal[num]=false;}
                }
            }
        }
    }
    // i determines which row/column/square. j determines which element of that thing.
    private int[] getsolvecoord(int i,int j,int index){
        if(index==0){return getrowcoord(i,j);}
        else if(index==1){return getcolcoord(i,j);}
        else if(index==2){return getsqcoord(i,j);}
        else{return null;}
    }
    private int[] getrowcoord(int i,int j){
        int[] coord={j,i}; return coord;
    }
    private int[] getcolcoord(int i,int j){
        int[] coord={i,j}; return coord;
    }
    private int[] getsqcoord(int i,int j){
        int[] coord={(i%sqsize)*sqsize+(j%sqsize),(i/sqsize)*sqsize+(j/sqsize)}; return coord;
    }
    // fill the grid with numbers (returns true if successful, false if it tried a fuckload of times and still nothing stuck)
    final public boolean fill(Random rand,int limit){
        debuglog("filling a puzzle with numbers");
        // method is far from foolproof -- loop keeps trying until something sticks
        boolean restart,failed=false;
        int tries=0;
        do{
            restart=false;
            // iterate through each position in the puzzle
            for(int x=0;x<sqsize2;x++){
                for(int y=0;y<sqsize2;y++){
                    // determine which numbers can go here
                    boolean[] testnum=new boolean[sqsize2+1];
                    int numgood=0;
                    for(int a=0;a<sqsize2;a++){
                        // check row
                        if(a!=x){testnum[getdata(a,y)]=true;}
                        // check column
                        if(a!=y){testnum[getdata(x,a)]=true;}
                        // check square
                        int i=((x/sqsize)*sqsize)+(a/sqsize);
                        int j=((y/sqsize)*sqsize)+(a%sqsize);
                        if(!(i==x && j==y)){testnum[getdata(i,j)]=true;}
                    }
                    for(int z=1;z<=sqsize2;z++){numgood+=testnum[z]?0:1;}
                    // assign a number, or restart if none can go here
                    if(numgood==0){
                        restart=true;
                        break;
                    }else{
                        int choice=rand.nextInt(numgood)+1;
                        for(int z=1;z<=sqsize2;z++){
                            choice-=testnum[z]?0:1;
                            if(choice==0){setdata(x,y,z);break;}
                        }
                    }
                }
                if(restart){break;}
            }
            tries++;
            if(restart && tries>=limit){debuglog("tried "+tries+" times to fill a puzzle, all failed. giving up.");failed=true;break;}
        }while(restart);
        debuglog("finished filling puzzle (took "+tries+" tries)");
        debuglog(toString());
        return !failed;
    }
    // make a puzzle from a filled grid. not a very optimal solution, but it works. returns the number of positions hidden.
    final public int hide(Random rand,int maxremoved,int maxcycles){
        debuglog("choosing which numbers to hide");
        int removed=0;
        // hide a bunch of numbers
        for(int cycles=0;cycles<maxcycles;cycles++){
            // select a random coord to try hiding
            int x=rand.nextInt(sqsize2);
            int y=rand.nextInt(sqsize2);
            if(!gethidden(x,y)){
                // check if the puzzle is still solveable with that new number missing
                sethidden(x,y,true);
                if(solve()){
                    removed++;
                    if(removed>=maxremoved){break;}
                }else{
                    sethidden(x,y,false);
                }
            }
        }
        debuglog(toString());
        return removed;
    }
    // getters and setters ("accessors" and "mutators" are *so* last week)
    // puzzle data
    private void setdata(int x,int y,int newdata){
        try{data[x+y*sqsize2]=newdata;}
        catch(Exception e){}
    }
    final public int getdata(int x,int y){
        try{return data[x+y*sqsize2];}
        catch(Exception e){return 0;}
    }
    // hidden numbers
    private void sethidden(int x,int y,boolean newdata){
        try{hidden[x+y*sqsize2]=newdata;}
        catch(Exception e){}
    }
    final public boolean gethidden(int x,int y){
        try{return hidden[x+y*sqsize2];}
        catch(Exception e){return false;}
    }
    // unique to solveable() method
    private void setdata(int[] array,int x,int y,int newdata){
        try{array[x+y*sqsize2]=newdata;}
        catch(Exception e){}
    }
    final public int getdata(int[] array,int x,int y){
        try{return array[x+y*sqsize2];}
        catch(Exception e){return 0;}
    }
    private void setlegal(boolean[][] array,int x,int y,boolean[] newdata){
        try{array[x+y*sqsize2]=newdata;}
        catch(Exception e){}
    }
    final public boolean[] getlegal(boolean[][] array,int x,int y){
        try{return array[x+y*sqsize2];}
        catch(Exception e){return null;}
    }
    private void sethidden(boolean[] array,int x,int y,boolean newdata){
        try{array[x+y*sqsize2]=newdata;}
        catch(Exception e){}
    }
    private boolean gethidden(boolean[] array,int x,int y){
        try{return array[x+y*sqsize2];}
        catch(Exception e){return false;}
    }
}
