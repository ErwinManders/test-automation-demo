package nl.rabobank.callforpapers;

import nl.rabobank.conference.Conference;

public class CallForPapersImpl implements CallForPapers
{
    @Override
    public void sendCFP(String name, Conference conference)
    {
        conference.handleCFP(name);
    }
}
