package org.example.vegapay;

public enum VegapayWorkflowState {
    PAN_VERIFIVATION, // In these specific state handles we can make api call for that vegapya state
                      // and save something in our db based on the response, but when reaching an
                      // intermidiate state if we have to fetch and store some info, let say if we
                      // have if they are processing some data async it
    Ekyc_Url_Generated,
    Current_Address_Details,
    Permanent_Address_Details,
    Ekyc_Verification,
    RE_Ekyc_Verification,
    Vkyc,
    Vkyc_Agent_Verification,
    Limit_Generate,
    Offer_Generated,
    Application_Rejected,

}
