package com.sismics.books.core.dao.jpa;

import com.google.common.base.Joiner;
import com.sismics.books.core.constant.Constants;
import com.sismics.books.core.dao.jpa.dto.UserDto;
import com.sismics.books.core.model.jpa.User;
import com.sismics.books.core.util.jpa.PaginatedList;
import com.sismics.books.core.util.jpa.PaginatedLists;
import com.sismics.books.core.util.jpa.QueryParam;
import com.sismics.books.core.util.jpa.SortCriteria;
import com.sismics.util.context.ThreadLocalContext;
import org.mindrot.jbcrypt.BCrypt;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.*;

/**
 * User DAO.
 * 
 * @author jtremeaux
 */
public class UserDao {
    /**
     * Authenticates an user.
     * 
     * @param email User login
     * @param password User password
     * @return ID of the authenticated user or null
     */
    public String authenticate(String email, String password) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        Query q = em.createQuery("select u from User u where u.email = :email and u.deleteDate is null");
        q.setParameter("email", email);
        try {
            User user = (User) q.getSingleResult();
            if (!BCrypt.checkpw(password, user.getPassword())) {
                return null;
            }
            return user.getId();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    /**
     * Creates a new user.
     * 
     * @param user User to create
     * @return User ID
     * @throws Exception
     */
    public String create(User user) throws Exception {
        // Create the user UUID
        user.setId(UUID.randomUUID().toString());
        
        // Checks for user unicity
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        Query q = em.createQuery("select u from User u where u.email = :email and u.deleteDate is null");
        q.setParameter("email", user.getEmail());
        List<?> l = q.getResultList();
        if (l.size() > 0) {
            throw new Exception("AlreadyExistingEmail");
        }
        
        user.setCreateDate(new Date());
        user.setPassword(hashPassword(user.getPassword()));
        user.setTheme(Constants.DEFAULT_THEME_ID);
        em.persist(user);
        
        return user.getId();
    }
    
    /**
     * Updates a user.
     * 
     * @param user User to update
     * @return Updated user
     */
    public User update(User user) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        
        // Get the user
        Query q = em.createQuery("select u from User u where u.id = :id and u.deleteDate is null");
        q.setParameter("id", user.getId());
        User userFromDb = (User) q.getSingleResult();

        // Update the user
        userFromDb.setLocaleId(user.getLocaleId());
        userFromDb.setEmail(user.getEmail());
        userFromDb.setTheme(user.getTheme());
        userFromDb.setFirstConnection(user.isFirstConnection());
        
        return user;
    }
    
    /**
     * Update the user password.
     * 
     * @param user User to update
     * @return Updated user
     */
    public User updatePassword(User user) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        
        // Get the user
        Query q = em.createQuery("select u from User u where u.id = :id and u.deleteDate is null");
        q.setParameter("id", user.getId());
        User userFromDb = (User) q.getSingleResult();

        // Update the user
        userFromDb.setPassword(hashPassword(user.getPassword()));
        
        return user;
    }

    /**
     * Gets a user by its ID.
     * 
     * @param id User ID
     * @return User
     */
    public User getById(String id) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        try {
            return em.find(User.class, id);
        } catch (NoResultException e) {
            return null;
        }
    }
    
    /**
     * Gets an active user by its username.
     * 
     * @param username User's username
     * @return User
     */
    public User getActiveByEmail(String email) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        try {
            Query q = em.createQuery("select u from User u where u.email = :email and u.deleteDate is null");
            q.setParameter("email", email);
            return (User) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    /**
     * Gets an active user by its password recovery token.
     * 
     * @param passwordResetKey Password recovery token
     * @return User
     */
    public User getActiveByPasswordResetKey(String passwordResetKey) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        try {
            Query q = em.createQuery("select u from User u where u.passwordResetKey = :passwordResetKey and u.deleteDate is null");
            q.setParameter("passwordResetKey", passwordResetKey);
            return (User) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    /**
     * Deletes a user.
     * 
     * @param email User's email
     */
    public void delete(String email) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
            
        // Get the user
        Query q = em.createQuery("select u from User u where u.email = :email and u.deleteDate is null");
        q.setParameter("email", email);
        User userFromDb = (User) q.getSingleResult();
        
        // Delete the user
        Date dateNow = new Date();
        userFromDb.setDeleteDate(dateNow);

        // Delete linked data
        q = em.createQuery("delete from AuthenticationToken at where at.userId = :userId");
        q.setParameter("userId", userFromDb.getId());
        q.executeUpdate();
    }

    /**
     * Hash the user's password.
     * 
     * @param password Clear password
     * @return Hashed password
     */
    protected String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
    
    /**
     * Returns the list of all users.
     * 
     * @param paginatedList List of users (updated by side effects)
     * @param sortCriteria Sort criteria
     */
    public void findAll(PaginatedList<UserDto> paginatedList, SortCriteria sortCriteria) {
        Map<String, Object> parameterMap = new HashMap<String, Object>();
        StringBuilder sb = new StringBuilder("select u.USE_ID_C as c0, u.USE_USERNAME_C as c1, u.USE_EMAIL_C as c2, u.USE_CREATEDATE_D as c3, u.USE_IDLOCALE_C as c4");
        sb.append(" from T_USER u ");
        
        // Add search criterias
        List<String> criteriaList = new ArrayList<String>();
        criteriaList.add("u.USE_DELETEDATE_D is null");
        
        if (!criteriaList.isEmpty()) {
            sb.append(" where ");
            sb.append(Joiner.on(" and ").join(criteriaList));
        }
        
        // Perform the search
        QueryParam queryParam = new QueryParam(sb.toString(), parameterMap);
        List<Object[]> l = PaginatedLists.executePaginatedQuery(paginatedList, queryParam, sortCriteria);
        
        // Assemble results
        List<UserDto> userDtoList = new ArrayList<UserDto>();
        for (Object[] o : l) {
            int i = 0;
            UserDto userDto = new UserDto();
            userDto.setId((String) o[i++]);
            userDto.setUsername((String) o[i++]);
            userDto.setEmail((String) o[i++]);
            userDto.setCreateTimestamp(((Timestamp) o[i++]).getTime());
            userDto.setLocaleId((String) o[i++]);
            userDtoList.add(userDto);
        }
        paginatedList.setResultList(userDtoList);
    }
}
