package botAlphaOmegaQuoter;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;

import core.BotCommand;
import core.BotInfo;
import core.MessageInfo;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.PostConstruct;

@RestController
public class BotAlphaOmegaQuoter {
    RestTemplate rest = new RestTemplate();
    private final static String name = "AlphaOmegaQuoter";
    private final static String displayName = "[" + name + "]";
    private final static String address = "http://localhost:8089";

    @PostConstruct
    public void init() {
        // Send authentication server bot information
        BotInfo info = new BotInfo(name, address);
        rest.put("http://localhost:8080/bot", info);
    }

    @RequestMapping(value = "/run", method = RequestMethod.PUT)
    public void getQuote(@RequestBody BotCommand command) { 
        int randomNum = ThreadLocalRandom.current().nextInt(0, quotes.length + 1);
        String quote = quotes[randomNum];
        MessageInfo response = new MessageInfo(displayName, quote);
        rest.put(command.responseAddress, response);
    }

    // A method for getting a heart beat from server
    @RequestMapping(value = "/test", method = RequestMethod.PUT)
    public void test() {}

    private final static String[] quotes = {
            "Someone come help me find my contact lens? And maybe restart my heart...?",
            "Do they make breath mints for dogs? They should...",
            "Well, don't look at me for answers. I have officially cleansed my hands of doing the right thing. We are now in the culpable hands of Nikolai. *claps slowly* Bravo, Nikolai.",
            "Sorry, but I'm already engaged to freedom.",
            "Come, mein little sturmgewehr, patients are waiting...",
            "Hello, comrade.",
            "Elemental shard is here somewhere. Only with it in my possession, can we take steps that must be taken.",
            "Whatever has become of my former allies, I must follow guidance of Kronorium. There is much to do, and much to be undone.",
            "So...I stand alone. Surely Richtofen has not somehow tricked me into shouldering this burden? Hmmm. That is exactly kind of thing he would do.",
            "Wake up, small town America.",
            "Go eat Dempsey! I hear he is made of hot dogs!",
            "Ita! That was my face!",
            "In death, all dogs are good dogs.",
            "I must search for ventilation units. They will not repair themselves...",
            "Electricity is our ally.",
            "Heads up! We got bandits on the roof!",
            "Other team members are currently MIA. Possibly AWOL. Will continue as planned and hopefully regroup before nightfall. Dempsey out.",
            "Mission priority is the recovery of an artifact known only as the Elemental Shard. Targets of opportunity will be assessed on a case by case basis.",
            "My bullet supply is running dry.",
            "MEMORANUM TO DIRECTOR CORNELIUS PERNELL",
            "Additional funding is to be provided to ensure the construction of the American Pyramid Device is completed on schedule.",
            "Congratulations, Director. God Bless America, and Merry Christmas.",
            "All Broken Arrow Operatives employed at the site must be vetted and approved by the Department of Defense and the Central Intelligence Agency.",
            "Now that Project MKALPHA is entereing PHASE II, we will begin to expand the scope of \"High-Risk\" Candidates for testing over the coming months.",
            "During this time K-642 should be fed meals at the appropriate intervals, but they are not to be spoken to, and they are not to be exposed to sunlight.",
            "Rushmore became operational. This learning computation machine is a first of its kind.",
            "When you have time, I highly recommend you stop by Operations and introduce yourself. ",
            "The Department of Defense has requested preliminary exloration of PROJECT TOY SOLDIER - repurposing an A.D.A.M. as a fully-functional military automoton.",
            "To access the PROJECT TOY SOLDIER Prototype (Codename: \"Sergeant A.D.A.M.\") please sumbit the code 7626 to Rushmore.",
            "They all failed, but I will be different. They believed their human form would cross the bridge to the higher plane. ",
            "They whispered words that soothed my soul. I was safe. I was secure. If I wished to reach Agartha, this was what I must do.",
            "Come now, Doctor Hale, this is Element 115 we're talking about here. We know what it's capable of.",
            "As if it's recharging itself. This ability... I think it's what Maxis was looking for all those years ago.",
            "Doctor Maxis spent years reanimating the dead. Hell, the Germans made that Group 935's primary purpose during the war.",
            "If we've done it, uh, I mean if we've truly restored life to the deceased... we'd better be damn sure we're right.",
            "Peter. His name was Peter McCain. I'll send you his file. And Doctor?",
            "Not a word of this to anyone outside the company. Not the CIA, not the DOD. Broken Arrow eyes only.",
            "Still a struggle, sir. If I close my eyes, I can see it clearly for a moment: Der Riese, Asylum, the Shi No Numa. But, then it's gone.",
            "Broken Arrow brought me back from beyond the veil, sir.",
            "We're not Group 935. We're not killing people to power your pyramid.",
            "I do not know... I just appear. I do what she is saying, I just appear!",
            "Not for long time... 50, maybe 60 years. There will be big boom, then she attack...",
            "What else did she show you, Yuri? What else have you seen?",
            "Four men: split in two. Beginning and end. First and last. Alpha and Omega. Primis and Ultimis.",
            "Get out of my head! The confluence! Gersh! The children!",
            "It is not for you! It is not for anything of us!",
            "I informed the staff that you've been experiencing delusions due to 115 exposure. You're a threat to yourself and all those around you.",
            "I carried the guilt of your death for years. I carried it for so long I was determined to find a way to bring you back.",
            "Cornelius, you're not well. You need help.",
            " Well you can't stop it Pernell. Sawyer will come, he'll bring his men, he'll see what you've become.",
            "Doctor, Director Pernell has betrayed his country and is considered an enemy of the state.",
            "Sir, we have a containment breach. Nova 6 gas is leaking throughout the facility.",
            "This is Case Officer Barkley. It's July 6th, 2025. Myself and Project Manager Russman have returned to Camp Edward",
            "Once the incident in '68 was contained, they powered down the site.",
            "Aww, come on now, Barkley. Robots ain't gonna harm anybody.",
            "Course it is. Ain't nothing breaking outta that tin can.",
            "Didn't you see the nuke they rigged up out there?",
            "If that thing were to ever escape... KABOOM! Up in smoke!",
            "Sample now transferred to Hanford Site.",
            "A large piece of rock that looks like glass and gives you a headache when you look at it?",
            "Anywho, what's this I hear about you leaving us for the CDC?",
            "I cannot triangulate your exact position. Edward, what is taking you so long?",
            "Dr. Monty suspects that you are acting against his wishes, plotting against him.",
            "We must discuss Monty's plan. If you are to follow through and deliver the souls, it will not resolve the paradox.",
            "Yes, the children will grow up and live in their perfect universe. But the blood vials: they will not break the cycle as you believe.",
            "I suspect Monty knows this. Perhaps the true cost of fixing all of reality is too great, even for him.",
            "Remember when the world was a simple place: No time travel, no multiverses, no paradoxes, no existential crises",
            "It is the Aether, Edward. Once it touches something, that thing is forever corrupted.",
            "We were doomed the moment Element 115 came to Earth.",
            "The Aether is inextricably linked with Agartha.",
            "Destroying Agartha... it would mean the end of him.",
            "Standby... wait on my signal. You will hear from me, very soon."};
}