package ru.simsonic.rscMessages;
import java.util.ArrayList;
import java.util.Random;

public class RowList
{
	// Database
	int     id;
	String  name;
	boolean enabled;
	boolean random;
	int     delay_sec;
	String  prefix;
	// Internal
	ArrayList<RowMessage> messages = new ArrayList<>();
	private static final Random rnd = new Random();
	public RowMessage getNextMessage(long currentTime)
	{
		if(messages.isEmpty())
			return null;
		if(random)
		{
			final ArrayList<RowMessage> veryOldMessages = new ArrayList<>();
			final ArrayList<RowMessage> enabledMessages = new ArrayList<>();
			long veryLongTime = 3 * messages.size() * 20 * delay_sec;
			for(RowMessage msg : messages)
				if(msg.enabled)
				{
					if(msg.lastBroadcast == 0 || (currentTime - msg.lastBroadcast) > veryLongTime)
						veryOldMessages.add(msg);
					enabledMessages.add(msg);
				}
			ArrayList<RowMessage> selectFrom = veryOldMessages.isEmpty() ? enabledMessages : veryOldMessages;
			return selectFrom.get(rnd.nextInt(selectFrom.size()));
		}
		RowMessage largestTime = messages.get(0);
		for(RowMessage msg : messages)
		{
			if(msg.enabled == false)
				continue;
			if(msg.lastBroadcast == 0)
				return msg;
			if(msg.lastBroadcast < largestTime.lastBroadcast)
				largestTime = msg;
		}
		return largestTime.enabled ? largestTime : null;
	}
}