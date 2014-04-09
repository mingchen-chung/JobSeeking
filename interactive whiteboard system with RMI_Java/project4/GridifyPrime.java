package project4;

import java.io.Serializable;
import java.lang.Long;
import java.util.StringTokenizer;
import java.util.Vector;
// gridify prime class, have define its own mapper/reducer
public class GridifyPrime implements Task<Long>, Serializable  
{
	private static final long serialVersionUID = 227L;
	
	private long RMIprime;
	private long RMImin;
	private long RMImax;
	
	public GridifyPrime(){}
	
	public long getPrime()
	{
		return RMIprime;
	}
	
	public void init(String value)
	{
		String cpValue = new String(value);
		StringTokenizer st = new StringTokenizer(cpValue, " \t\r\n");
		
		this.RMIprime = new Long(st.nextToken());
		this.RMImin = new Long(st.nextToken());
		this.RMImax = new Long(st.nextToken());
	}
	
	public Long check_prime(long prime, long min, long max)
	{
		//for(long divisor = min ; divisor <= max ; divisor++)
		for(long divisor = max ; divisor >= min ; divisor--)
		{
			if(divisor == 1 || divisor == prime)
				continue;
			if(prime % divisor == 0) 
				return divisor;
		}
		return new Long(1);
	}
	
	public Vector<GridifyPrime> primeMapper(int num)
	{	
		Vector<GridifyPrime> vect = new Vector<GridifyPrime>();
		// man, min should be cut via RMImax and RMImin
		long interval = (RMImax - RMImin) / num;
		long min, max;
		
		for(int i = 0 ; i < num ; i++)
		{
			GridifyPrime prime = new GridifyPrime();
			min = i * interval + RMImin;
			max = (i == (num - 1) ? RMImax : (i + 1) * interval + RMImin);
			prime.init(RMIprime + " " + min + " " + max);
			vect.add(prime);
		}
		
		return vect;
	}
	
	public Long primeReducer(Vector<Long> mapped_results)
	{
		System.err.println("part1");
		Long result = new Long(1);
		System.err.println("part2");
		for(int i = 0 ; i < mapped_results.size() ; i++)
		{
			System.err.println("part3 " + mapped_results.get(i) + " " + result + " " + mapped_results.size());
			if(mapped_results.get(i) == 1)
				continue;
			else
			{
				System.err.println("part4");
				// result is maximun or minimun divisor, so don't break Immediately
				result = ((Long)mapped_results.get(i) > result) ? (Long)mapped_results.get(i): result;
				//break;
			}
		}
		System.err.println("part5");
		return result;
	}
	// execute with Gridify annotation
	@Gridify(mapper = "primeMapper", reducer = "primeReducer")
	public Long execute()
	{
		return check_prime(RMIprime, RMImin, RMImax);
	}
}
