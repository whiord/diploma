(define (domain philosophers)
    (:requirements :typing :negative-preconditions)
    (:types
      Philosopher - object
      Fork - object
    )
    (:predicates
      (right ?phi - Philosopher ?for - Fork)
      (left ?phi - Philosopher ?for - Fork)
      (hold ?phi - Philosopher ?for - Fork)
      (hungry ?phi - Philosopher)
      (hasLeft ?phi - Philosopher)
      (eat ?phi - Philosopher)
      (think ?phi - Philosopher)
      (hasRight ?phi - Philosopher)
    )
    (:action getLeft
     :parameters (?p1 - Philosopher ?p2 - Philosopher ?f - Fork)
     :precondition 
       (and
         (left ?p1 ?f)
         (right ?p2 ?f)
         (hungry ?p1)
         (not (hold ?p2 ?f))
       )
     :effect
       (and
         (hasLeft ?p1)
         (hold ?p1 ?f)
         (not (hungry ?p1))
       )
    )

    (:action getRight
     :parameters (?p1 - Philosopher ?p2 - Philosopher ?f - Fork)
     :precondition 
       (and
         (right ?p1 ?f)
         (left ?p2 ?f)
         (hasLeft ?p1)
         (not (hold ?p2 ?f))
       )
     :effect
       (and
         (not (hasLeft ?p1))
         (hold ?p1 ?f)
         (eat ?p1)
       )
    )

    (:action goHungry
     :parameters (?p - Philosopher)
     :precondition 
       (think ?p)
     :effect
       (and
         (not (think ?p))
         (hungry ?p)
       )
    )

    (:action releaseLeft
     :parameters (?p - Philosopher ?f - Fork)
     :precondition 
       (and
         (eat ?p)
         (hold ?p ?f)
         (left ?p ?f)
       )
     :effect
       (and
         (not (hold ?p ?f))
         (not (eat ?p))
         (hasRight ?p)
       )
    )

    (:action releaseRight
     :parameters (?p - Philosopher ?f - Fork)
     :precondition 
       (and
         (hold ?p ?f)
         (right ?p ?f)
         (hasRight ?p)
       )
     :effect
       (and
         (think ?p)
         (not (hold ?p ?f))
         (not (hasRight ?p))
       )
    )

)
