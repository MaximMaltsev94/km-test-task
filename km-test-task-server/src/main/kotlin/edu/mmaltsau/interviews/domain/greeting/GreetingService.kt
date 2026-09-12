package edu.mmaltsau.interviews.domain.greeting

interface GreetingService {
    fun sayBasicHello(): String
    fun sayPersonalHello(personal: String): PersonalGreeting
}

class StaticGreetingService : GreetingService {
    override fun sayBasicHello(): String {
        return "Hello, World!";
    }

    override fun sayPersonalHello(personal: String): PersonalGreeting {
        return PersonalGreeting(
            personal,
            "Hello to ${personal}!"
        )
    }

}