package commonClasses;

/**
 * @author Farhad Omar
 * 		   National Institute of Standards and Technology
 *		   farhad.omar@nist.gov
 */

public class SingleObjectDataTransfer extends UIInputData{
	
	/*This class is using the Singleton pattern and inherits UIInputData class for capturing UI inputs*/
	
	/********** This implementation is thread-safe, see Java concurrency in practice, Chapter 16. Page 347..*/
	// creating an object of SingleObjectDataTransfer
	private static SingleObjectDataTransfer instance = null;
	
	// Making the constructor private so no other object of this class can be instantiated
	private SingleObjectDataTransfer(){}
	
	// Return only one instance of this object. The method is thread safe.
	public synchronized static SingleObjectDataTransfer getInstance() {
		if (instance == null){
			instance = new SingleObjectDataTransfer();
		}
		return instance;
	}
	
}
