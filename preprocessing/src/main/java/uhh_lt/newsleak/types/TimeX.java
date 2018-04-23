package uhh_lt.newsleak.types;

public class TimeX {
	int beginOffset;
	int endOffset;
	String timeX;
	String timeXType;
	String timexValue;

	public TimeX(int aBeginOffset, int aEndOffset, String aTimeX, String aTimexType, String aTimexValue) {
		this.beginOffset = aBeginOffset;
		this.endOffset = aEndOffset;
		this.timeX = aTimeX;
		this.timeXType = aTimexType;
		this.timexValue = aTimexValue;

	}
}