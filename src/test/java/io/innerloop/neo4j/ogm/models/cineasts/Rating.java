package io.innerloop.neo4j.ogm.models.cineasts;

import io.innerloop.neo4j.ogm.annotations.RelationshipProperties;

/**
 * Created by markangrish on 07/05/2015.
 */
@RelationshipProperties
public class Rating
{
    private static final int MAX_STARS = 5;

    private static final int MIN_STARS = 0;

    private int stars;

    private String comment;

    public Rating()
    {
    }

    public Rating(int stars, String comment)
    {
        if (stars >= MIN_STARS && stars <= MAX_STARS)
            this.stars = stars;
        if (comment != null && !comment.isEmpty())
            this.comment = comment;
    }

    public int getStars()
    {
        return stars;
    }

    public String getComment()
    {
        return comment;
    }
}
