package ericvh.TDT;

/**
 * Class  Newton contains the logic required to perform Netwon's method of 
 * iterative root finding on single variable functions.
 * @author Eric
 * Created on 2 juli 2007, 22:34
 *  Revision:
 */

public class Newton
{
    
    // nobody should construct a "Newton"
    private Newton()
    {
    }
    
    /**
     * Newton.Result is a record type which aggregates the results of evaluating
     * both a function at a point and its derriviative.
     **/
    public final static class Result
    {
        public final double f;
        public final double f_prime;
        
        /**
         * @param f *  effects creates a new Result record with the given values
         * @param f_prime
         **/
        public Result(double f, double f_prime)
        {
            this.f = f;
            this.f_prime = f_prime;
        }
        
        /**
         * @return  returns true if either f or f' is NaN
         **/
        public boolean undefined()
        {
            return Double.isNaN(f) || Double.isNaN(f_prime);
        }
        
        /**
         * @param other *  @returns true if this result's sign is not identical
         * to other's
         **/
        public boolean funcSignChance(Result other)
        {
            return (f * other.f <= 0);
        }
        
        /** 
         * returns true if this result's derivative's sign is not identical
         * to other's 
         **/
        public boolean derivativeSignChance(Result other)
        {
            return (f_prime * other.f_prime <= 0);
        }
        
        /** 
         * returns funcSignChange() || derivativeSignChange() 
         **/
        public boolean signChange(Result other)
        {
            return (f * other.f <= 0) || (f_prime * other.f_prime <= 0);
        }
        
        @Override
        public String toString()
        {
            return "f(t)= " + f + "; f'(t)= " + f_prime + "";
        }
    }
    
    /** Convenience name for an undefined function result
     **/
    public static final Result UNDEFINED = new Result(Double.NaN, Double.NaN);
    /**
     * Newton.Function is an interface which specifies a function whose roots 
     * can be found by this class.
     **/
    public static interface Function
    {
        /**
         * returns the value of the function evaluated at t.
         **/
        public abstract Result evaluate(double t);
    }
    
    private static final int MAX_STEPS = 30;
    private static final double epsilon =  0.000000001;
    
    //requires t_min "<= t_max; t_step" > 0
    /**
     * Searches for possible roots of function by looking for sign changes.  If a possible root is found
     * newton's method is performed to try to determine the precise value of the root. This function searches
     * for roots between t_min and t_max  with a step size of t_step.  If no solution is found returns NaN, 
     * else returns the solution.
     *
     * Note: The returned value may not actually be within these bounds.
     */
    public static double findRoot(Function function,   double t_min,   double t_max,   double t_step)
    {
        // initialize to NaN so that sign-change is initially false
        Result eval = UNDEFINED;
        
        for (double t = t_min; t < t_max + t_step; t += t_step)
        {
            Result old = eval;
            eval = function.evaluate(t);
            // System.out.println("at " + t + " " + evan);
            
            if (eval.undefined())
            {
                continue;
            }
            
            // did f or f' change sign?
            if (eval.signChange(old))
            {
                double root = findRoot(function,  (old.f_prime <= 0) ? t - t_step : t);
                // check to make sure it was within the bounds of this check
                if ((t - t_step <= root) && (root <= t))
                {
                    // System.out.println("returning " + root);
                    return root;
                }
            }
        }
        
        return Double.NaN;
    }
    
    /**
     * Performs netwon's method on function starting at initial_t and returns the solution.  If no solution
     * is found, returns NaN
     **/
    public static double findRoot(Function function,  double initial_t)
    {
        double t = initial_t;
        
        for (int count = 0; count < MAX_STEPS; count++)
        {
            Result eval = function.evaluate(t);
            if (eval.undefined())
            {
                return Double.NaN;
            }
            
            double t_next = t - eval.f/eval.f_prime;
            
            if (Math.abs(t_next - t) < epsilon)
            {
                if (Math.abs(eval.f) < 1000*epsilon)
                {
                    // claim it's close enough to call a hit (as opposed to a local minima)
                    return t_next;
                }
                else
                {
                    return Double.NaN;
                }
            }
            
            t = t_next;
        }
        return Double.NaN;
    }
       
    /** curveFit : 
     * 
     */
    private void curveFit()
    {
        long N = 0;
        double X = 0.0;
        double sumX = 0.0, sumY  = 0.0, sumXX  = 0.0, sumXY = 0.0, sXX  = 0.0, sXY = 0.0;
        double intercept, slope;
        
        for (int i = 0; i < N; i++)
        {
            double valueY = 0.0 ;
            
            ++N;
             X = StrictMath.log(N);
            sumX += N;
            sumXY += valueY * N;
            sumXX += N * N;
            sumY += valueY;
        }
        
        sXX = sumXX - sumX * sumX / N;
        sXY = sumXY - sumY * sumX / N;
        slope = sXY / sXX;
        intercept = ((sumXX * sumY - sumX * sumXY) / N ) / sXX;    
        double yCalc = intercept + slope * X;
    }
    
}

