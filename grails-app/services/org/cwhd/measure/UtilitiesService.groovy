package org.cwhd.measure

import grails.transaction.Transactional
import org.apache.commons.logging.LogFactory
import java.text.SimpleDateFormat

/**
 * general utilities as static helper methods
 */
@Transactional
class UtilitiesService {
    private static final logger = LogFactory.getLog(this)

    /**
     * take the @ out of emails so elasticsearch doesn't index everyone's email domain
     * @param author string representing the original email
     * @return string with the clean, EC index friendly email
     */
    static String cleanEmail(author) {
        def result
        if(author) {
            result = author.replace("@", "_")
        }

        return result
    }

    /**
     * User names from JIRA have spaces, dashes, and other strange things that will mess up the EC index.
     * This method cleans all that junk off
     * @param userName
     * @return
     */
    static String makeNonTokenFriendly(userName) {
        def result
        if(userName) {
            result = userName.replace("_etw","").replace("_whq","").replace(" ", "_").replace("(","").replace(")","").replace(",","").replace("-","")
        }
        return result
    }

    /**
     * get a data from a JIRA REST response and turn it into a real date
     * @param date string that represents a date from a JIRA response
     * @return a java date
     */
    static Date cleanJiraDate(date) {
        def result
        //2014-06-17T18:02:24.000+0000
        if(date) {
            result = date.replace("T","")
            result = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss.SSSZ").parse(result)
        }
        return result
    }

    /**
     * this method will tell you if your estimates are high or low.
     *         //get the exact time ratio for the upper & lower bounds
     //find the difference between the 2
     //divide that by 2
     * @param estimate the original estimate on the card
     * @param actualTime how long (in days) the task took to complete
     * @param maxEstimate the highest possible estimate for your team
     * @param maxTime the most time your team can spend on a task.  This should be the length of your dev cycle in days
     * @param estimationValues the possible values that your team can estimate.  This is typcially the beginning of the fibonacci series.
     * @return a value of 0 indicates that you're good.  Greater than 0 means underestimating, less than 0 indicated overestimating.
     */
    //TODO if we have all the estimate values then we really don't need the max estimate passed in...
    static def estimateHealth(estimate, actualTime, maxEstimate, maxTime, estimationValues) {
        if(!estimationValues) {
            estimationValues = [1, 2, 3, 5, 8, 13]
        }
        def result
        def timeEstimateRatio =  maxTime / maxEstimate
        def estimateTime = estimate * timeEstimateRatio
        def upperTimeBound = maxTime
        def lowerTimeBound = 0

        logger.debug("estimate: $estimate | estimateTime: $estimateTime | actualTime: $actualTime")

        def currentEstimateIndex = estimationValues.findIndexOf { it == estimate}
        //calculate the lowerTimeBound
        if(currentEstimateIndex == 0) {
            lowerTimeBound = 0
        } else {
            lowerTimeBound = estimateTime - ((estimateTime - (estimationValues[estimationValues.findIndexOf { it == estimate} - 1] * timeEstimateRatio)) / 2)
            logger.debug("lowerTimeBound: $lowerTimeBound")
        }
        //calculate the upperTimeBound
        if (currentEstimateIndex == estimationValues.size() -1) {
            upperTimeBound = maxTime
        } else {
            upperTimeBound = estimateTime + (((estimationValues[estimationValues.findIndexOf { it == estimate} + 1] * timeEstimateRatio) - estimateTime) / 2)
            logger.debug("upperTimeBound: $upperTimeBound | ")
        }

        //Calculate the result
        if(upperTimeBound < actualTime) {   //this took longer then it should have, it was underestimated
            def diff = actualTime - upperTimeBound
            result = 0 + diff
            //TODO normalize the number
        } else if(lowerTimeBound > actualTime) { //this took less time then estimated, it was overestimated
            def diff = lowerTimeBound - actualTime
            result = 0 - diff
            //TODO normalize the number
        } else {
            result = 0
        }

        logger.debug("*************************************")
        logger.debug("result = $result")
        logger.debug("*************************************")

        return [ raw:result, result:result.toInteger() ]
    }
}
