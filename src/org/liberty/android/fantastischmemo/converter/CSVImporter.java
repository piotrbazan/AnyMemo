/*
Copyright (C) 2010 Haowen Ning

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/
package org.liberty.android.fantastischmemo.converter;

import java.io.File;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.Callable;

import org.liberty.android.fantastischmemo.*;

import java.io.FileReader;
import java.util.LinkedList;

import org.liberty.android.fantastischmemo.dao.CardDao;
import org.liberty.android.fantastischmemo.dao.CategoryDao;
import org.liberty.android.fantastischmemo.dao.LearningDataDao;

import org.liberty.android.fantastischmemo.domain.Card;
import org.liberty.android.fantastischmemo.domain.Category;
import org.liberty.android.fantastischmemo.domain.LearningData;
import au.com.bytecode.opencsv.CSVReader;

import android.content.Context;

public class CSVImporter implements AbstractConverter{
    private Context mContext;

    public CSVImporter(Context context){
        mContext = context;
    }

    public void convert(String src, String dest) throws Exception{
        new File(dest).delete();
        AnyMemoDBOpenHelper helper = AnyMemoDBOpenHelperManager.getHelper(mContext, dest);
        try {
            final CardDao cardDao = helper.getCardDao();
            final CategoryDao categoryDao = helper.getCategoryDao();
            final LearningDataDao learningDataDao = helper.getLearningDataDao();

            CSVReader reader = new CSVReader(new FileReader(src));
            String[] nextLine;
            int count = 0;
            final List<Card> cardList = new LinkedList<Card>();
            while((nextLine = reader.readNext()) != null) {
                if(nextLine.length < 2){
                    throw new Exception("Malformed CSV file. Please make sure the CSV's first column is question, second one is answer and the optinal third one is category");
                }
                count++;
                String note = "";
                String category = "";
                if(nextLine.length >= 3){
                    category = nextLine[2];
                }
                if(nextLine.length >= 4){
                    note = nextLine[3];
                }
                Card card = new Card();
                Category cat = new Category();
                LearningData ld = new LearningData();
                cat.setName(category);

                card.setOrdinal(count);
                card.setCategory(cat);
                card.setLearningData(ld);
                card.setQuestion(nextLine[0]);
                card.setAnswer(nextLine[1]);
                card.setNote(note);
                cardList.add(card);
            }


            cardDao.callBatchTasks(new Callable<Void>() {
                // Use the map to get rid of duplicate category creation
                final Map<String, Category> categoryMap = new HashMap<String, Category>();
                public Void call() throws Exception {
                    for (Card card : cardList) {
                        String currentCategoryName = card.getCategory().getName();
                        if (categoryMap.containsKey(currentCategoryName)) {
                            card.setCategory(categoryMap.get(currentCategoryName));
                        } else {
                            categoryDao.create(card.getCategory());
                            categoryMap.put(currentCategoryName, card.getCategory());
                        }
                        learningDataDao.create(card.getLearningData());
                        cardDao.create(card);
                    }
                    return null;
                }
            });
        } finally {
            AnyMemoDBOpenHelperManager.releaseHelper(dest);
        }
    }
}

